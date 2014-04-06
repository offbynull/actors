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
package com.offbynull.peernetic.nettyp2p.handlers.selfblock;

import com.offbynull.peernetic.nettyp2p.handlers.read.IncomingMessage;
import com.offbynull.peernetic.nettyp2p.handlers.read.ReadToQueueHandler;
import com.offbynull.peernetic.nettyp2p.handlers.xstream.XStreamDecodeHandler;
import com.offbynull.peernetic.nettyp2p.handlers.xstream.XStreamEncodeHandler;
import com.offbynull.peernetic.nettyp2p.helpers.SimpleChannelBuilder;
import com.offbynull.peernetic.nettyp2p.helpers.SimpleChannelBuilder.SimpleChannel;
import com.offbynull.peernetic.nettyp2p.simulation.PerfectLine;
import com.offbynull.peernetic.nettyp2p.simulation.TransitPacketRepository;
import io.netty.channel.DefaultAddressedEnvelope;
import java.net.InetSocketAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SelfBlockTest {

    public SelfBlockTest() {
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
        LinkedBlockingQueue<IncomingMessage> recverQueue = new LinkedBlockingQueue<>();
        
        TransitPacketRepository packetRepository = TransitPacketRepository.create(new PerfectLine());
        SelfBlockId id = new SelfBlockId();

        SimpleChannel senderCh = new SimpleChannelBuilder()
                .simulatedUdp(new InetSocketAddress("1.1.1.1", 1), packetRepository)
                .handlers(new SelfBlockIdPrependHandler(id),
                        new XStreamEncodeHandler())
                .build();
        SimpleChannel recverCh = new SimpleChannelBuilder()
                .simulatedUdp(new InetSocketAddress("2.2.2.2", 2), packetRepository)
                .handlers(new SelfBlockIdCheckHandler(id, false),
                        new XStreamDecodeHandler())
                .funnelReadsToQueue(recverQueue)
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
        LinkedBlockingQueue<IncomingMessage> recverQueue = new LinkedBlockingQueue<>();
        
        TransitPacketRepository packetRepository = TransitPacketRepository.create(new PerfectLine());
        SelfBlockId senderId = new SelfBlockId();
        SelfBlockId recverId = new SelfBlockId();

        SimpleChannel senderCh = new SimpleChannelBuilder()
                .simulatedUdp(new InetSocketAddress("1.1.1.1", 1), packetRepository)
                .handlers(new SelfBlockIdPrependHandler(senderId),
                        new XStreamEncodeHandler())
                .build();
        SimpleChannel recverCh = new SimpleChannelBuilder()
                .simulatedUdp(new InetSocketAddress("2.2.2.2", 2), packetRepository)
                .handlers(new SelfBlockIdCheckHandler(recverId, false),
                        new XStreamDecodeHandler())
                .funnelReadsToQueue(recverQueue)
                .build();

        
        Object sendMsg = "hello world";
        IncomingMessage recvMsg;
        
        senderCh.writeAndFlush(new DefaultAddressedEnvelope<>(sendMsg, new InetSocketAddress("2.2.2.2", 2)));
        recvMsg = recverQueue.poll(1, TimeUnit.SECONDS);
        
        Assert.assertEquals(sendMsg, recvMsg.getMessage());
        
        senderCh.close();
        recverCh.close();
        packetRepository.close();
    }
}
