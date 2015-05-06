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
package com.offbynull.peernetic.core.gateways.recorder;

import com.offbynull.peernetic.core.actor.Actor;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.core.common.Serializer;
import com.offbynull.peernetic.core.gateway.Gateway;
import com.offbynull.peernetic.core.gateway.InputGateway;
import java.io.File;
import org.apache.commons.lang3.Validate;

/**
 * {@link Gateway} that forwards incoming messages to a configured destination address and saves those messages to a file. The source
 * address of the message is kept intact, meaning that the source address of the message does not change to the address of this gateway once
 * it's forwarded. The destination address of the message moves over the address suffix once it's forwarded.
 * <p>
 * In the following example, there are two {@link Actor}s: {@code sender} and {@code echoer}.
 * <ul>
 * <li>{@code sender} sends messages to some address and expects that message to be echo'd back to it.</li>
 * <li>{@code echoer} echoes incoming messages back to the sender.</li>
 * </ul>
 * A {@link RecorderGateway} instance (hence forth called {@code recorder}) is configured to save all incoming messages and forward those
 * messages to {@code echoer}. When {@code sender} sends a message, it's configured to send it to {@code recorder} rather than directly to
 * {@code echoer}. When {@code recorder} forwards incoming messages ...
 * <ul>
 * <li>the source address is kept in tact, meaning that the forwarded messages {@code echoer} receives are sent as if they were sent
 * directly by {@code sender}. As such, any replies from {@code echoer} to {@code sender} will go directly to {@code sender}.</li>
 * <li>the destination address maintains the address suffix, meaning that if a message was sent to {@code recorder:special:suffix:here}, the
 * destination address for the forwarded message would be {@code echoer:special:suffix:here}.</li>
 * </ul>
 * <pre>
 * CountDownLatch latch = new CountDownLatch(1);
 * 
 * // Define the sender actor
 * Coroutine sender = (cnt) -&gt; {
 *     Context ctx = (Context) cnt.getContext();
 *     String dstAddr = ctx.getIncomingMessage();
 * 
 *     for (int i = 0; i &lt; 10; i++) {
 *         ctx.addOutgoingMessage(dstAddr, i);
 *         cnt.suspend();
 *         Validate.isTrue(i == (int) ctx.getIncomingMessage());
 *     }
 * 
 *     // Breaks actor implementation rules to have this here, but it's fine for the purposes of this example
 *     latch.countDown();
 * };
 * 
 * // Define the echoer actor
 * Coroutine echoer = (cnt) -&gt; {
 *     Context ctx = (Context) cnt.getContext();
 * 
 *     while (true) {
 *         String src = ctx.getSource();
 *         Object msg = ctx.getIncomingMessage();
 *         ctx.addOutgoingMessage(src, msg);
 *         System.out.println(msg);
 *         cnt.suspend();
 *     }
 * };
 * 
 * // Create actor threads
 * ActorThread echoerThread = ActorThread.create("echoer");
 * ActorThread senderThread = ActorThread.create("sender");
 * 
 * // Create recorder that records events coming to echoer and then passes it along to echoer
 * RecorderGateway echoRecorderGateway = RecorderGateway.record(
 *         "recorder",
 *         echoerThread.getIncomingShuttle(),
 *         "echoer:echoer",
 *         eventsFile,
 *         new SimpleSerializer());
 * Shuttle echoRecorderShuttle = echoRecorderGateway.getIncomingShuttle();
 * 
 * 
 * // Wire sender to send to echoerRecorder instead of echoer
 * senderThread.addOutgoingShuttle(echoRecorderShuttle);
 * 
 * // Wire echoer to send back directly to recorder
 * echoerThread.addOutgoingShuttle(senderThread.getIncomingShuttle());
 * 
 * // Add coroutines
 * echoerThread.addCoroutineActor("echoer", echoer);
 * senderThread.addCoroutineActor("sender", sender, "recorder");
 * 
 * // Wait until sender actor finishes
 * latch.await();
 * 
 * // Terminate recorder
 * echoRecorderGateway.close();
 * echoRecorderGateway.await();
 * </pre>
 * @author Kasra Faghihi
 * @see ReplayerGateway
 */
public final class RecorderGateway implements InputGateway {
    private final Thread writeThread;
    private final RecorderShuttle recorderShuttle;
    
    /**
     * Creates a {@link RecorderGateway} instance. Note that there is no notification mechanism to let you know that this recorder has
     * terminated (whether from an error or from a call to {@link #close() }).
     * @param prefix address prefix of the destination
     * @param dstShuttle shuttle to replay events to
     * @param dstAddress address to replay events to
     * @param file file to read events from
     * @param serializer serializer used to deserialize events
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code dstAddress} is not a prefix of address returned by {@code dstShuttle}
     * @return new {@link ReplayerGateway} instance
     */
    public static RecorderGateway record(String prefix, Shuttle dstShuttle, String dstAddress, File file, Serializer serializer) {
        Validate.notNull(prefix);
        Validate.notNull(dstShuttle);
        Validate.notNull(dstAddress);
        Validate.notNull(file);
        Validate.notNull(serializer);
        Validate.isTrue(AddressUtils.isPrefix(dstShuttle.getPrefix(), dstAddress));
        
        WriteRunnable writeRunnable = new WriteRunnable(file, prefix, serializer);
        WriteBus writeBus = writeRunnable.getBus();
        RecorderShuttle recorderShuttle = new RecorderShuttle(prefix, writeBus, dstShuttle, dstAddress);
        
        Thread writeThread = new Thread(writeRunnable);
        writeThread.setDaemon(true);
        writeThread.setName(RecorderGateway.class.getSimpleName());
        
        RecorderGateway ret = new RecorderGateway(writeThread, recorderShuttle);
        
        writeThread.start();        
        
        return ret;
    }

    private RecorderGateway(Thread writeThread, RecorderShuttle recorderShuttle) {
        Validate.notNull(writeThread);
        Validate.notNull(recorderShuttle);

        this.writeThread = writeThread;
        this.recorderShuttle = recorderShuttle;
    }

    
    @Override
    public Shuttle getIncomingShuttle() {
        return recorderShuttle;
    }

    /**
     * Blocks until this {@link RecorderGateway} terminates.
     * @throws InterruptedException if interrupted while blocking
     */
    public void await() throws InterruptedException {
        writeThread.join();
    }
    
    @Override
    public void close() {
        writeThread.interrupt(); // thread will close the bus when it gets an exception
    }
    
}
