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

import com.offbynull.actors.core.shuttle.Shuttle;
import com.offbynull.actors.core.shuttles.simple.Bus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WebRunnable implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(WebRunnable.class);

    private final Map<String, Shuttle> outgoingShuttles;
    private final String prefix;
    private final Bus bus;

    public WebRunnable(String prefix, Bus bus) {
        Validate.notNull(prefix);
        Validate.notNull(bus);
        this.outgoingShuttles = new HashMap<>();
        this.prefix = prefix;
        this.bus = bus;
    }

    @Override
    public void run() {
//        Server server = null;
//        
        try {
//            server = new Server(8080);
//            
//            ResourceHandler resourceHandler = new ResourceHandler();
//            resourceHandler.setDirectoriesListed(false);
//            resourceHandler.setWelcomeFiles(new String[]{ "index.html" });
//            // set useCaches to false if running this embedded jetty in an external web container
//            resourceHandler.setBaseResource(Resource.newClassPathResource("/webui", true, true));
//            ContextHandler resourceContextHandler = new ContextHandler();
//            resourceContextHandler.setContextPath("/webui");
//            resourceContextHandler.setHandler(resourceHandler);
//            
//            ServletHandler servletHandler = new ServletHandler();
//            
//            MessageGatewayServlet mgServlet = new MessageGatewayServlet(prefix, bus);
//            ServletHolder servletHolder = new ServletHolder(mgServlet);
//            servletHolder.setAsyncSupported(true);
//            servletHandler.addServletWithMapping(servletHolder, "/webgateway");
//            
////            DoSFilter dosFilter = new DoSFilter();
////            dosFilter.setMaxRequestsPerSec(30);
////            FilterHolder filterHolder = new FilterHolder(dosFilter);
////            servletHandler.addFilterWithMapping(filterHolder, "/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC));
//
//            HandlerList handlers = new HandlerList();
//            handlers.setHandlers(new Handler[] { resourceContextHandler, servletHandler, new DefaultHandler() });
//            
//            server.setHandler(handlers);
//            server.start();
            
            while (true) {
                // Poll for new messages
                List<Object> incomingObjects = bus.pull();

                Validate.notNull(incomingObjects);
                Validate.noNullElements(incomingObjects);

                // Queue new messages
                for (Object incomingObj : incomingObjects) {
                    LOG.debug("Processing management message: {} ", incomingObj);
                    if (incomingObj instanceof AddShuttle) {
                        AddShuttle addShuttle = (AddShuttle) incomingObj;
                        Shuttle shuttle = addShuttle.getShuttle();
                        Shuttle existingShuttle = outgoingShuttles.putIfAbsent(shuttle.getPrefix(), shuttle);
                        Validate.validState(existingShuttle == null);
                    } else if (incomingObj instanceof RemoveShuttle) {
                        RemoveShuttle removeShuttle = (RemoveShuttle) incomingObj;
                        String prefix = removeShuttle.getPrefix();
                        Shuttle oldShuttle = outgoingShuttles.remove(prefix);
                        Validate.validState(oldShuttle != null);
                    }
                }
            }
        } catch (InterruptedException ie) {
            LOG.debug("Web gateway interrupted");
            Thread.interrupted();
        } catch (Exception e) {
            LOG.error("Internal error encountered", e);
        } finally {
            bus.close();
        }
    }

}
