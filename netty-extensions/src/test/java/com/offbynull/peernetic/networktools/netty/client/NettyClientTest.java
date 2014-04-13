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
package com.offbynull.peernetic.networktools.netty.client;

import com.offbynull.peernetic.nettyextensions.builders.NettyClient;
import com.offbynull.peernetic.nettyextensions.builders.NettyClientBuilder;
import com.offbynull.peernetic.nettyextensions.channels.simulatedpacket.PerfectLine;
import com.offbynull.peernetic.nettyextensions.channels.simulatedpacket.TransitPacketRepository;
import com.offbynull.peernetic.nettyextensions.handlers.readwrite.Message;
import com.offbynull.peernetic.nettyextensions.handlers.selfblock.SelfBlockId;
import com.offbynull.peernetic.nettyextensions.handlers.selfblock.SelfBlockIdCheckHandler;
import com.offbynull.peernetic.nettyextensions.handlers.selfblock.SelfBlockIdPrependHandler;
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

public final class NettyClientTest {

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
                .addReadDestination(recverQueue)
                .build();

        Object sendMsg = "hello world";
        Object recvMsg;

        senderCh.channel().writeAndFlush(new DefaultAddressedEnvelope<>(sendMsg, new InetSocketAddress("2.2.2.2", 2)));
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
                .addReadDestination(recverQueue)
                .build();

        Object sendMsg = "hello world";
        Message recvMsg;

        senderCh.channel().writeAndFlush(new DefaultAddressedEnvelope<>(sendMsg, new InetSocketAddress("2.2.2.2", 2)));
        recvMsg = recverQueue.poll(1, TimeUnit.SECONDS);

        Assert.assertEquals(sendMsg, recvMsg.getMessage());

        senderCh.close();
        recverCh.close();
        packetRepository.close();
    }

    @Test
    public void ensureMessagesFromQueueAreWritten() throws Exception {
        List<Message> messages = Arrays.asList(
                new Message(null, new InetSocketAddress("2.2.2.2", 2), "hi!", null),
                new Message(null, new InetSocketAddress("2.2.2.2", 2), "how are you?", null),
                new Message(null, new InetSocketAddress("2.2.2.2", 2), "bye!", null));
        
        LinkedBlockingQueue<Message> senderQueue = new LinkedBlockingQueue<>(messages);
        LinkedBlockingQueue<Message> recverQueue = new LinkedBlockingQueue<>();

        TransitPacketRepository packetRepository = TransitPacketRepository.create(new PerfectLine());

        NettyClient recverCh = new NettyClientBuilder()
                .simulatedUdp(new InetSocketAddress("2.2.2.2", 2), packetRepository)
                .addReadDestination(recverQueue)
                .build();
        NettyClient senderCh = new NettyClientBuilder()
                .simulatedUdp(new InetSocketAddress("1.1.1.1", 1), packetRepository)
                .addWriteSource(senderQueue, 50L)
                .build();
        
        Thread.sleep(1000L);

        Message incomingMsg;
        
        incomingMsg = recverQueue.poll();
        Assert.assertEquals(messages.get(0).getMessage(), incomingMsg.getMessage());
        Assert.assertEquals(new InetSocketAddress("1.1.1.1", 1), incomingMsg.getRemoteAddress());
        Assert.assertEquals(new InetSocketAddress("2.2.2.2", 2), incomingMsg.getLocalAddress());
        
        incomingMsg = recverQueue.poll();
        Assert.assertEquals(messages.get(1).getMessage(), incomingMsg.getMessage());
        Assert.assertEquals(new InetSocketAddress("1.1.1.1", 1), incomingMsg.getRemoteAddress());
        Assert.assertEquals(new InetSocketAddress("2.2.2.2", 2), incomingMsg.getLocalAddress());
        
        incomingMsg = recverQueue.poll();
        Assert.assertEquals(messages.get(2).getMessage(), incomingMsg.getMessage());
        Assert.assertEquals(new InetSocketAddress("1.1.1.1", 1), incomingMsg.getRemoteAddress());
        Assert.assertEquals(new InetSocketAddress("2.2.2.2", 2), incomingMsg.getLocalAddress());
        
        incomingMsg = recverQueue.poll();
        Assert.assertNull(incomingMsg);
        
        senderCh.close();
        recverCh.close();
        packetRepository.close();
    }
    
    @Test
    public void simpleUdpTest() throws Exception {
        final String msg1 = "hi!";
        final String msg2 = "how are you?";
        final String msg3 = "bye!";
        
        LinkedBlockingQueue<Message> ch1SenderQueue = new LinkedBlockingQueue<>(Arrays.asList(
                new Message(null, new InetSocketAddress("127.0.0.1", 22222), msg1, null),
                new Message(null, new InetSocketAddress("127.0.0.1", 22222), msg2, null),
                new Message(null, new InetSocketAddress("127.0.0.1", 22222), msg3, null)));
        LinkedBlockingQueue<Message> ch2SenderQueue = new LinkedBlockingQueue<>(Arrays.asList(
                new Message(null, new InetSocketAddress("127.0.0.1", 11111), msg1, null),
                new Message(null, new InetSocketAddress("127.0.0.1", 11111), msg2, null),
                new Message(null, new InetSocketAddress("127.0.0.1", 11111), msg3, null)));
        LinkedBlockingQueue<Message> ch1RecverQueue = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<Message> ch2RecverQueue = new LinkedBlockingQueue<>();

        NettyClient ch1 = new NettyClientBuilder()
                .udp(new InetSocketAddress("127.0.0.1", 11111))
                .addWriteSource(ch1SenderQueue, 50L)
                .addReadDestination(ch1RecverQueue)
                .build();
        NettyClient ch2 = new NettyClientBuilder()
                .udp(new InetSocketAddress("127.0.0.1", 22222))
                .addWriteSource(ch2SenderQueue, 50L)
                .addReadDestination(ch2RecverQueue)
                .build();
        
        Thread.sleep(1000L);

        
        Message incomingMsg;
        
        // check channel 1
        incomingMsg = ch1RecverQueue.poll();
        Assert.assertEquals(msg1, incomingMsg.getMessage());
        Assert.assertEquals(new InetSocketAddress("127.0.0.1", 22222), incomingMsg.getRemoteAddress());
        Assert.assertEquals(new InetSocketAddress("127.0.0.1", 11111), incomingMsg.getLocalAddress());
        
        incomingMsg = ch1RecverQueue.poll();
        Assert.assertEquals(msg2, incomingMsg.getMessage());
        Assert.assertEquals(new InetSocketAddress("127.0.0.1", 22222), incomingMsg.getRemoteAddress());
        Assert.assertEquals(new InetSocketAddress("127.0.0.1", 11111), incomingMsg.getLocalAddress());
        
        incomingMsg = ch1RecverQueue.poll();
        Assert.assertEquals(msg3, incomingMsg.getMessage());
        Assert.assertEquals(new InetSocketAddress("127.0.0.1", 22222), incomingMsg.getRemoteAddress());
        Assert.assertEquals(new InetSocketAddress("127.0.0.1", 11111), incomingMsg.getLocalAddress());
        
        incomingMsg = ch1RecverQueue.poll();
        Assert.assertNull(incomingMsg);
        
        
        
        // check channel 2
        incomingMsg = ch2RecverQueue.poll();
        Assert.assertEquals(msg1, incomingMsg.getMessage());
        Assert.assertEquals(new InetSocketAddress("127.0.0.1", 22222), incomingMsg.getLocalAddress());
        Assert.assertEquals(new InetSocketAddress("127.0.0.1", 11111), incomingMsg.getRemoteAddress());
        
        incomingMsg = ch2RecverQueue.poll();
        Assert.assertEquals(msg2, incomingMsg.getMessage());
        Assert.assertEquals(new InetSocketAddress("127.0.0.1", 22222), incomingMsg.getLocalAddress());
        Assert.assertEquals(new InetSocketAddress("127.0.0.1", 11111), incomingMsg.getRemoteAddress());
        
        incomingMsg = ch2RecverQueue.poll();
        Assert.assertEquals(msg3, incomingMsg.getMessage());
        Assert.assertEquals(new InetSocketAddress("127.0.0.1", 22222), incomingMsg.getLocalAddress());
        Assert.assertEquals(new InetSocketAddress("127.0.0.1", 11111), incomingMsg.getRemoteAddress());
        
        incomingMsg = ch2RecverQueue.poll();
        Assert.assertNull(incomingMsg);
        
        
        
        ch1.close();
        ch2.close();
    }
}
