/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.gateways.visualizer;

import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.gateway.InputGateway;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import com.offbynull.peernetic.core.shuttles.simple.SimpleShuttle;
import java.util.function.Supplier;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.commons.lang3.Validate;

public final class GraphGateway implements InputGateway {

    private final Thread thread;
    private final Bus bus;
    
    private final SimpleShuttle shuttle;

    public GraphGateway(String prefix) {
        Validate.notNull(prefix);

        bus = new Bus();
        shuttle = new SimpleShuttle(prefix, bus);
        thread = new Thread(new GraphRunnable(bus));
        thread.setDaemon(true);
        thread.setName(getClass().getSimpleName() + "-" + prefix);
        thread.start();
    }

    @Override
    public Shuttle getIncomingShuttle() {
        return shuttle;
    }

    @Override
    public void close() {
        thread.interrupt();
        exitApplication();
    }

    public static void startApplication() {
        Validate.isTrue(GraphApplication.getInstance() == null);
        Thread thread = new Thread(() -> Application.launch(GraphApplication.class));
        thread.setDaemon(true);
        thread.setName(GraphGateway.class.getName() + " thread");
        thread.start();
        
        try {
            GraphApplication.awaitStarted();
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    public void addStage(Supplier<? extends Stage> factory) {
        Validate.notNull(factory);
        bus.add(new CreateStage(factory));
    }

    public static void awaitShutdown() {
        try {
            GraphApplication.awaitStopped();
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static void exitApplication() {
        Platform.exit();
    }
}
