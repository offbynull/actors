/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.network.client;

import com.offbynull.peernetic.network.handlers.readwrite.Message;
import com.offbynull.peernetic.network.handlers.selfblock.SelfBlockId;
import com.offbynull.peernetic.network.handlers.selfblock.SelfBlockIdCheckHandler;
import com.offbynull.peernetic.network.handlers.selfblock.SelfBlockIdPrependHandler;
import com.offbynull.peernetic.network.simulation.PerfectLine;
import com.offbynull.peernetic.network.simulation.TransitPacketRepository;
import io.netty.channel.DefaultAddressedEnvelope;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class NettyClientTest {

    public NettyClientTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void ensureBlockedOnSameIdTest() throws Exception {
        LinkedBlockingQueue<Message> recverQueue = new LinkedBlockingQueue<>();

        TransitPacketRepository packetRepository = TransitPacketRepository.create(new PerfectLine());
        SelfBlockId id = new SelfBlockId();

        NettyClient senderCh = new NettyClientBuilder()
                .simulatedUdp(new InetSocketAddress("1.1.1.1", 1), packetRepository)
                .customHandlers(new SelfBlockIdPrependHandler(id))
                .build();
        NettyClient recverCh = new NettyClientBuilder()
                .simulatedUdp(new InetSocketAddress("2.2.2.2", 2), packetRepository)
                .customHandlers(new SelfBlockIdCheckHandler(id, false))
                .readTo(recverQueue)
                .build();

        Object sendMsg = "hello world";
        Object recvMsg;

        senderCh.writeAndFlush(new DefaultAddressedEnvelope<>(sendMsg, new InetSocketAddress("2.2.2.2", 2)));
        recvMsg = recverQueue.poll(1, TimeUnit.SECONDS);

        Assert.assertNull(recvMsg);

        senderCh.close();
        recverCh.close();
        packetRepository.close();
    }

    @Test
    public void ensurePassOnDifferentIdTest() throws Exception {
        LinkedBlockingQueue<Message> recverQueue = new LinkedBlockingQueue<>();

        TransitPacketRepository packetRepository = TransitPacketRepository.create(new PerfectLine());
        SelfBlockId senderId = new SelfBlockId();
        SelfBlockId recverId = new SelfBlockId();

        NettyClient senderCh = new NettyClientBuilder()
                .simulatedUdp(new InetSocketAddress("1.1.1.1", 1), packetRepository)
                .includeSelfBlockId(senderId)
                .build();
        NettyClient recverCh = new NettyClientBuilder()
                .simulatedUdp(new InetSocketAddress("2.2.2.2", 2), packetRepository)
                .checkForSelfBlockId(recverId)
                .readTo(recverQueue)
                .build();

        Object sendMsg = "hello world";
        Message recvMsg;

        senderCh.writeAndFlush(new DefaultAddressedEnvelope<>(sendMsg, new InetSocketAddress("2.2.2.2", 2)));
        recvMsg = recverQueue.poll(1, TimeUnit.SECONDS);

        Assert.assertEquals(sendMsg, recvMsg.getMessage());

        senderCh.close();
        recverCh.close();
        packetRepository.close();
    }

    @Test
    public void ensureMessagesFromQueueAreWritten() throws Exception {
        List<Message> messages = Arrays.asList(
                new Message(null, new InetSocketAddress("2.2.2.2", 2), "hi!"),
                new Message(null, new InetSocketAddress("2.2.2.2", 2), "how are you?"),
                new Message(null, new InetSocketAddress("2.2.2.2", 2), "bye!"));
        
        LinkedBlockingQueue<Message> senderQueue = new LinkedBlockingQueue<>(messages);
        LinkedBlockingQueue<Message> recverQueue = new LinkedBlockingQueue<>();

        TransitPacketRepository packetRepository = TransitPacketRepository.create(new PerfectLine());

        NettyClient senderCh = new NettyClientBuilder()
                .simulatedUdp(new InetSocketAddress("1.1.1.1", 1), packetRepository)
                .writeFrom(senderQueue, 50L)
                .build();
        NettyClient recverCh = new NettyClientBuilder()
                .simulatedUdp(new InetSocketAddress("2.2.2.2", 2), packetRepository)
                .readTo(recverQueue)
                .build();
        
        Thread.sleep(1000L);
        
        Assert.assertEquals(messages.get(0).getMessage(), recverQueue.poll().getMessage());
        Assert.assertEquals(messages.get(1).getMessage(), recverQueue.poll().getMessage());
        Assert.assertEquals(messages.get(2).getMessage(), recverQueue.poll().getMessage());
        
        senderCh.close();
        recverCh.close();
        packetRepository.close();
    }
}
