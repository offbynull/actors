/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.actors.core.gateway.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.offbynull.actors.core.gateway.servlet.MessageCache.MessageBlock;
import com.offbynull.actors.core.shuttle.Message;
import com.offbynull.actors.core.shuttle.Shuttle;
import com.offbynull.actors.core.shuttles.simple.Bus;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.Validate;

final class MessageGatewayServlet extends HttpServlet {
    
    private final Gson gson;
    
    private final MessageCache messageCache;

    private final ConcurrentHashMap<String, Shuttle> outgoingShuttles;
    private final Bus toHttpBus;
    
    MessageGatewayServlet(String prefix, ConcurrentHashMap<String, Shuttle> outgoingShuttles, Bus toHttpBus, long sessionTimeout) {
        Validate.notNull(prefix);
        Validate.notNull(outgoingShuttles);
        Validate.notNull(toHttpBus);
        Validate.isTrue(sessionTimeout > 0L);

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Message.class, new MessageJsonDeserializer(prefix));
        gsonBuilder.registerTypeAdapter(Message.class, new MessageJsonSerializer(prefix));
        gsonBuilder.registerTypeAdapter(HttpToSystemBundle.class, new HttpToSystemBundleJsonDeserializer(prefix));
        gsonBuilder.registerTypeAdapter(SystemToHttpBundle.class, new SystemToHttpBundleJsonSerializer(prefix));
        gson = gsonBuilder.serializeNulls().create();
        
        this.messageCache = new InMemoryMessageCache(sessionTimeout);

        this.outgoingShuttles = outgoingShuttles;
        this.toHttpBus = toHttpBus;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // Read incoming messages
            Reader reader = req.getReader();
            HttpToSystemBundle httpToSystemBundle = gson.fromJson(reader, HttpToSystemBundle.class);
            
            String id = httpToSystemBundle.getHttpAddressId();
            
            
            
            
            // Update last access time for ID
            messageCache.keepAlive(id);
            
            
            
                        
            // Remove messages that the client says it's recv'd
            messageCache.systemToHttpAcknowledge(id, httpToSystemBundle.getSystemToHttpOffset());
            
            
            
            

            // Processing incoming messages
            messageCache.httpToSystemAdd(id,
                    httpToSystemBundle.getHttpToSystemOffset(),
                    httpToSystemBundle.getMessages());
            
            MessageBlock httpToSystemMessages = messageCache.httpToSystemRead(id);
            
            for (Message message : httpToSystemMessages.getMessages()) {
                String dstPrefix = message.getDestinationAddress().getElement(0);
                Shuttle dstShuttle = outgoingShuttles.get(dstPrefix);
                
                if (dstShuttle != null) {
                    dstShuttle.send(message);
                }
            }
            
            messageCache.httpToSystemClear(id);

            

            
            // Push out outgoing messages
            List<Message> rawSystemToHttpMessages = (List<Message>) toHttpBus.pull(0L, TimeUnit.NANOSECONDS).stream();
            messageCache.systemToHttpAppend(id, rawSystemToHttpMessages);
            
            MessageBlock systemToHttpMessages = messageCache.httpToSystemRead(id);
            
            int systemToHttpOffset = systemToHttpMessages.getStartSequenceOffset();
            int httpToSystemOffset = httpToSystemMessages.getStartSequenceOffset();
            SystemToHttpBundle systemToHttpBundle = new SystemToHttpBundle(
                    id,
                    systemToHttpOffset,
                    httpToSystemOffset,
                    systemToHttpMessages.getMessages());
            Writer writer = resp.getWriter();
            gson.toJson(systemToHttpBundle, writer);
        } catch (InterruptedException ie) {
            Thread.interrupted(); // clear interrupted status
            throw new ServletException(ie);
        }
    }
}
