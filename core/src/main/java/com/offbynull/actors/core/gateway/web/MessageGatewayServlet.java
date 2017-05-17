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
import com.offbynull.actors.core.shuttle.Message;
import com.offbynull.actors.core.shuttles.simple.Bus;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.Validate;

final class MessageGatewayServlet extends HttpServlet {
    
    private final Gson gson;
    
    private final HttpToSystemMessageFilter httpToSystemMessageFilter;
    private final SystemToHttpMessageCache systemToHttpMessageCache;
    
    private final String prefix;
    private final Bus toSystemBus;
    private final Bus toHttpBus;
    
    public MessageGatewayServlet(String prefix, Bus toSystemBus, Bus toHttpBus) {
        Validate.notNull(prefix);
        Validate.notNull(toSystemBus);

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Message.class, new MessageJsonDeserializer(prefix));
        gsonBuilder.registerTypeAdapter(HttpToSystemBundle.class, new HttpToSystemBundleJsonDeserializer(prefix));
        gsonBuilder.registerTypeAdapter(SystemToHttpBundle.class, new SystemToHttpBundleJsonSerializer(prefix));
        gson = gsonBuilder.create();
        
        this.httpToSystemMessageFilter = new HttpToSystemMessageFilter(prefix, 60000L);
        this.systemToHttpMessageCache = new SystemToHttpMessageCache(prefix, 60000L);
        
        this.prefix = prefix;
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
            
            // The incoming message bundle includes an acknowledgement -- this is the max sequence number of the messages it was able to
            // read. We remove any messages older than that here.
            systemToHttpMessageCache.filter(
                    time,
                    httpToSystemBundle.getHttpAddressId(),
                    httpToSystemBundle.getSystemToHttpOffset());
            
            // The incoming message bundle includes messages that may have already arrived (e.g. we got the messages but the system may have
            // crashed) -- we track this by sequence number. If the sequence number for a message is older than the latest one we've got,
            // we filter it out here.
            List<Message> filteredHttpToSystemMessages =
                    httpToSystemMessageFilter.filter(
                            time,
                            httpToSystemBundle.getHttpToSystemOffset(),
                            httpToSystemBundle.getMessages());
            
            // Send these messages (however many are left after we've filterd out the ones that have already arrived) to the system.
            toSystemBus.add(filteredHttpToSystemMessages);

            

            
            // Get new messages and add them to cache
            List<Message> rawSystemToHttpMessages = (List<Message>) toHttpBus.pull(0L, TimeUnit.NANOSECONDS).stream();
            systemToHttpMessageCache.add(time, rawSystemToHttpMessages);
            
            // Get pending messages for this http address
            SortedMap<Long, Message> systemToHttpMessages =
                    systemToHttpMessageCache.read(
                            time,
                            httpToSystemBundle.getHttpAddressId());
            
            // Create and send out bundle
            long systemToHttpOffset = systemToHttpMessages.firstKey();
            long httpToSystemOffset = httpToSystemMessageFilter.latestSequence(httpToSystemBundle.getHttpAddressId());
            SystemToHttpBundle systemToHttpBundle =
                    new SystemToHttpBundle(
                            systemToHttpOffset,
                            httpToSystemOffset,
                            new ArrayList<>(systemToHttpMessages.values()));
            Writer writer = resp.getWriter();
            gson.toJson(systemToHttpBundle, writer);
        } catch (InterruptedException ie) {
            Thread.interrupted(); // clear interrupted status
            throw new ServletException(ie);
        }
    }
}
