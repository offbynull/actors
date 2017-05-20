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
package com.offbynull.actors.core.gateway.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.offbynull.actors.core.gateway.web.MessageCache.MessageBlock;
import com.offbynull.actors.core.shuttle.Message;
import com.offbynull.actors.core.shuttles.simple.Bus;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.Validate;

final class MessageGatewayServlet extends HttpServlet {
    
    private final Gson gson;
    
    private final MessageCache messageCache;

    private final Bus toSystemBus;
    private final Bus toHttpBus;
    
    public MessageGatewayServlet(String prefix, Bus toSystemBus, Bus toHttpBus) {
        Validate.notNull(prefix);
        Validate.notNull(toSystemBus);

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Message.class, new MessageJsonDeserializer(prefix));
        gsonBuilder.registerTypeAdapter(Message.class, new MessageJsonSerializer(prefix));
        gsonBuilder.registerTypeAdapter(HttpToSystemBundle.class, new HttpToSystemBundleJsonDeserializer(prefix));
        gsonBuilder.registerTypeAdapter(SystemToHttpBundle.class, new SystemToHttpBundleJsonSerializer(prefix));
        gson = gsonBuilder.serializeNulls().create();
        
        this.messageCache = new InMemoryMessageCache(60000L);

        this.toSystemBus = toSystemBus;
        this.toHttpBus = toHttpBus;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            long time = System.currentTimeMillis();
            
            
            // Read incoming messages
            Reader reader = req.getReader();
            HttpToSystemBundle httpToSystemBundle = gson.fromJson(reader, HttpToSystemBundle.class);
            
            String id = httpToSystemBundle.getHttpAddressId();
            
            
            
            
            // Update last access time for ID
            messageCache.keepAlive(id);
            
            
            
            
            // The incoming message bundle includes an acknowledgement -- this is the max sequence number of the messages it was able to
            // read. We remove any messages older than that here.
            messageCache.httpToSystemAdd(id,
                    httpToSystemBundle.getHttpToSystemOffset(),
                    httpToSystemBundle.getMessages());
            
            // The incoming message bundle includes messages that may have already arrived (e.g. we got the messages but the system may have
            // crashed) -- we track this by sequence number. If the sequence number for a message is older than the latest one we've got,
            // we filter it out here.
            MessageBlock httpToSystemMessages = messageCache.httpToSystemRead(id);
            
            // Send these messages (however many are left after we've filterd out the ones that have already arrived) to the system.
            toSystemBus.add(httpToSystemMessages.getMessages());
            
            // Clear out the messages we just sent from the cache.
            messageCache.httpToSystemClear(id);

            

            
            // Get new messages and add them to cache
            List<Message> rawSystemToHttpMessages = (List<Message>) toHttpBus.pull(0L, TimeUnit.NANOSECONDS).stream();
            messageCache.systemToHttpAppend(id, rawSystemToHttpMessages);
            
            // Get pending messages for this http address
            MessageBlock systemToHttpMessages = messageCache.httpToSystemRead(id);
            
            // Create and send out bundle
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
