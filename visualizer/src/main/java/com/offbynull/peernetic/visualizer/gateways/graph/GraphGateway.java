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
package com.offbynull.peernetic.visualizer.gateways.graph;

import com.offbynull.peernetic.core.actor.Actor;
import com.offbynull.peernetic.core.gateway.Gateway;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.gateway.InputGateway;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import com.offbynull.peernetic.core.shuttles.simple.SimpleShuttle;
import java.util.function.Supplier;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.commons.lang3.Validate;

/**
 * {@link Gateway} that provides access to 2D graphs built on top of JavaFX.
 * <p>
 * In the following example, the {@link Actor} called {@code tester} sends messages to the {@link GraphGateway} called {@code graph} such
 * that it creates two graphs: {@code g1} and {@code g2}. {@code g1} has the nodes {@code [n1, n2, n3]} added and linked, while {@code g2}
 * has the nodes {@code [e1, e2]} added and linked. Each node is positioned and styled.
 * <p>
 * The user can use the UI to show and hide individual graphs.
 * <pre>
 * Coroutine tester = (cnt) -&gt; {
 *     Context ctx = (Context) cnt.getContext();
 *
 *     Address graphPrefix = ctx.getIncomingMessage();
 *     
 *     // Create graph g1
 *     ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new AddNode("n1"));
 *     ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new LabelNode("n1", "new label for\n\nn1"));
 *     ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new MoveNode("n1", 0.0, 0.0));
 *     ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new StyleNode("n1", 0xFF0000));
 *     ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new AddNode("n2"));
 *     ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new MoveNode("n2", 200.0, 0.0));
 *     ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new StyleNode("n2", 0x00FF00));
 *     ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new AddNode("n3"));
 *     ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new MoveNode("n3", 0.0, 200.0));
 *     ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new StyleNode("n3", 0x0000FF));
 *     ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new AddEdge("n1", "n2"));
 *     ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new AddEdge("n2", "n3"));
 *     ctx.addOutgoingMessage(graphPrefix.appendSuffix("g1"), new AddEdge("n3", "n1"));

 *     // Create graph g2
 *     ctx.addOutgoingMessage(graphPrefix.appendSuffix("g2"), new AddNode("e1"));
 *     ctx.addOutgoingMessage(graphPrefix.appendSuffix("g2"), new MoveNode("e1", 0.0, 0.0));
 *     ctx.addOutgoingMessage(graphPrefix.appendSuffix("g2"), new StyleNode("e1", 0xFF00FF));
 *     ctx.addOutgoingMessage(graphPrefix.appendSuffix("g2"), new AddNode("e2"));
 *     ctx.addOutgoingMessage(graphPrefix.appendSuffix("g2"), new MoveNode("e2", 200.0, 200.0));
 *     ctx.addOutgoingMessage(graphPrefix.appendSuffix("g2"), new StyleNode("e2", 0x00FFFF));
 *     ctx.addOutgoingMessage(graphPrefix.appendSuffix("g2"), new AddEdge("e1", "e2"));
 * };
 *
 * GraphGateway graphGateway = new GraphGateway("graph");
 * Shuttle graphInputShuttle = graphGateway.getIncomingShuttle();
 * GraphGateway.startApplication();
 *
 * ActorThread testerThread = ActorThread.create("local");
 *
 * testerThread.addOutgoingShuttle(graphInputShuttle);
 * testerThread.addCoroutineActor("tester", tester, Address.of("graph"));
 *
 * GraphGateway.awaitShutdown();
 * </pre>
 * @author Kasra Faghihi
 */
public final class GraphGateway implements InputGateway {

    private final Thread thread;
    private final Bus bus;
    
    private final SimpleShuttle shuttle;

    /**
     * Constructs a {@link GraphGateway} instance.
     * @param prefix address prefix for this gateway
     * @throws NullPointerException if any argument is {@code null}
     */
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

    /**
     * Start the JavaFX application.
     * @throws IllegalStateException if application is already running
     */
    public static void startApplication() {
        Validate.validState(GraphApplication.getInstance() == null);
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

    /**
     * Set the {@link GraphNodeAddHandler} and {@link GraphNodeRemoveHandler} that graphs should use.
     * @param nodeAddHandler node add handler
     * @param nodeRemoveHandler node remove handler
     * @throws NullPointerException if any argument is {@code null}
     */
    public void setHandlers(GraphNodeAddHandler nodeAddHandler, GraphNodeRemoveHandler nodeRemoveHandler) {
        Validate.notNull(nodeAddHandler);
        Validate.notNull(nodeRemoveHandler);
        bus.add(new UpdateHandlers(nodeAddHandler, nodeRemoveHandler));
    }
    
    /**
     * Add a custom {@link Stage} to the JavaFX application.
     * @param factory factory to create stage
     * @throws NullPointerException if any argument is {@code null}
     */
    public void addStage(Supplier<? extends Stage> factory) {
        Validate.notNull(factory);
        bus.add(new CreateStage(factory));
    }

    /**
     * Wait for the JavaFX application to be closed.
     * @throws IllegalStateException if thread is interrupted while waiting
     */
    public static void awaitShutdown() {
        try {
            GraphApplication.awaitStopped();
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Exits the JavaFX application. Equivalent to calling {@link Platform#exit() }.
     */
    public static void exitApplication() {
        Platform.exit();
    }
}
