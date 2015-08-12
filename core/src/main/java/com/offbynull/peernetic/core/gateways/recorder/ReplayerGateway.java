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
import com.offbynull.peernetic.core.common.Serializer;
import com.offbynull.peernetic.core.gateway.Gateway;
import com.offbynull.peernetic.core.shuttle.Address;
import java.io.File;
import org.apache.commons.lang3.Validate;

/**
 * {@link Gateway} that replays events saved by a {@link RecorderGateway}.
 * <p>
 * In the following example, there's a single {@link Actor}: {@code echoer}. A {@link ReplayerGateway} instance (hence forth called
 * {@code replayer}) is configured to read recorded incoming messages from a file and send them to {@code echoer}. This example essentially
 * replays the recording performed in the example for {@link RecorderGateway}.
 * <pre>
 * Coroutine echoer = (cnt) -&gt; {
 *     Context ctx = (Context) cnt.getContext();
 * 
 *     // Normally, actors shouldn't be logging to System.out or doing any other IO. They're logging to System.out here for simplicity. If 
 *     // you need to do logging in your actor, use LogGateway instead.
 *     while (true) {
 *         String src = ctx.getSource();
 *         Object msg = ctx.getIncomingMessage();
 *         ctx.addOutgoingMessage(src, msg);
 *         System.out.println(msg);
 *         cnt.suspend();
 *     }
 * };
 * 
 * ActorRunner echoerRunner = ActorRunner.create("echoer");
 * 
 * // Wire echoer to send back to null
 * echoerRunner.addOutgoingShuttle(new NullShuttle("sender"));
 * 
 * // Add coroutines
 * echoerRunner.addCoroutineActor("echoer", echoer);
 * 
 * // Create replayer that mocks out sender and replays previous events to echoer
 * ReplayerGateway replayerGateway = ReplayerGateway.replay(
 *         echoerRunner.getIncomingShuttle(),
 *         "echoer:echoer",
 *         eventsFile,
 *         new SimpleSerializer());
 * replayerGateway.await();
 * </pre>
 * @author Kasra Faghihi
 * @see RecorderGateway
 */
public final class ReplayerGateway implements Gateway {

    private final Thread readThread;
    
    /**
     * Creates a {@link ReplayerGateway} instance that immediately starts replaying events read from a file. Note that there is no
     * notification mechanism to let you know that this replayer has terminated (whether from an error or from end-of-file).
     * @param dstShuttle shuttle to replay events to
     * @param dstAddress address to replay events to
     * @param file file to read events from
     * @param serializer serializer used to deserialize events
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code dstAddress} is not a prefix of address returned by {@code dstShuttle}
     * @return new {@link ReplayerGateway} instance
     */
    public static ReplayerGateway replay(Shuttle dstShuttle, Address dstAddress, File file, Serializer serializer) {
        Validate.notNull(dstShuttle);
        Validate.notNull(dstAddress);
        Validate.notNull(file);
        Validate.notNull(serializer);
        Validate.isTrue(!dstAddress.isEmpty());
        Validate.isTrue(Address.of(dstShuttle.getPrefix()).isPrefixOf(dstAddress));
        
        ReadRunnable readRunnable = new ReadRunnable(dstShuttle, dstAddress, file, serializer);
        Thread readThread = new Thread(readRunnable);
        readThread.setDaemon(true);
        readThread.setName(RecorderGateway.class.getSimpleName());
        
        ReplayerGateway ret = new ReplayerGateway(readThread);
        
        readThread.start();        
        
        return ret;
    }

    private ReplayerGateway(Thread readThread) {
        this.readThread = readThread;
    }
    
    /**
     * Blocks until this {@link ReplayerGateway} terminates.
     * @throws InterruptedException if interrupted while blocking
     */
    public void await() throws InterruptedException {
        readThread.join();
    }

    @Override
    public void close() throws InterruptedException {
        readThread.interrupt();
        readThread.join();
    }

    
}
