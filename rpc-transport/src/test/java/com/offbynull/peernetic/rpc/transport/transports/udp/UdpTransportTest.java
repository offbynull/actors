/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.rpc.transport.transports.udp;

import com.offbynull.peernetic.common.concurrent.actor.ActorQueue;
import com.offbynull.peernetic.common.concurrent.actor.Message;
import com.offbynull.peernetic.rpc.transport.actormessages.commands.SendRequestCommand;
import com.offbynull.peernetic.rpc.transport.actormessages.commands.SendResponseCommand;
import com.offbynull.peernetic.rpc.transport.actormessages.events.RequestArrivedEvent;
import com.offbynull.peernetic.rpc.transport.actormessages.events.ResponseArrivedEvent;
import com.offbynull.peernetic.rpc.transport.common.IncomingMessageManager.IncomingResponse;
import com.offbynull.peernetic.rpc.transport.common.OutgoingMessageManager.OutgoingResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public final class UdpTransportTest {
    
    private static final long TIMEOUT_DURATION = 500;
    private static final int BUFFER_SIZE = 500;
    private static final int ID_CACHE_SIZE = 1024;

    private UdpTransport transport1;
    private UdpTransport transport2;
    private int port1;
    private int port2;
    private static AtomicInteger nextPort;

    public UdpTransportTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        nextPort = new AtomicInteger(12000);
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws IOException {
        port1 = nextPort.getAndIncrement();
        transport1 = new UdpTransport(new InetSocketAddress(InetAddress.getLocalHost(), port1), BUFFER_SIZE, ID_CACHE_SIZE, TIMEOUT_DURATION);
        
        port2 = nextPort.getAndIncrement();
        transport2 = new UdpTransport(new InetSocketAddress(InetAddress.getLocalHost(), port2), BUFFER_SIZE, ID_CACHE_SIZE, TIMEOUT_DURATION);
    }

    @After
    public void tearDown() throws IOException {
    }

    @Test
    public void selfUdpTest() throws Throwable {
        ActorQueue fakeQueue = new ActorQueue();
                    
        InetSocketAddress to = new InetSocketAddress(InetAddress.getLocalHost(), port1);
        byte[] reqData = "HIEVERYBODY! :)".getBytes();
        byte[] respData = "THIS IS THE RESPONSE".getBytes();
        Message msg;
        Iterator<Message> msgIt;

        try {
            transport1.setDestinationWriter(fakeQueue.getWriter());
            transport1.start();
            
            
            // write message to self
            msg = Message.createRespondableMessage(0, fakeQueue.getWriter(),
                    new SendRequestCommand<>(to, ByteBuffer.wrap(reqData)));
            transport1.getWriter().push(msg);
            
            // wait for self to get msg
            msgIt = fakeQueue.getReader().pull(2000L);
            Assert.assertTrue(msgIt.hasNext());
            
            msg = msgIt.next();
            RequestArrivedEvent<InetSocketAddress> requestArrivedEvent = (RequestArrivedEvent<InetSocketAddress>) msg.getContent();
            
            Assert.assertEquals(ByteBuffer.wrap(reqData), requestArrivedEvent.getData());
            Assert.assertEquals(to, requestArrivedEvent.getFrom());
            
            // respond to self
            msg.getResponder().respondImmediately(new SendResponseCommand<>(ByteBuffer.wrap(respData)));
            
            // wait for self to get resp
            msgIt = fakeQueue.getReader().pull(2000L);
            Assert.assertTrue(msgIt.hasNext());

            msg = msgIt.next();
            ResponseArrivedEvent<InetSocketAddress> responseArrivedEvent = (ResponseArrivedEvent<InetSocketAddress>) msg.getContent();
            
            Assert.assertEquals(ByteBuffer.wrap(respData), responseArrivedEvent.getData());
            Assert.assertEquals(to, responseArrivedEvent.getFrom());
        } finally {
            transport1.stop();
        }
        
        msgIt = fakeQueue.getReader().pull(0L);
        Assert.assertFalse(msgIt.hasNext());
    }

//    @Test
//    public void normalUdpTest() throws Throwable {
//        IncomingMessageListener<InetSocketAddress> listener = new IncomingMessageListener<InetSocketAddress>() {
//
//            @Override
//            public void messageArrived(IncomingMessage<InetSocketAddress> message, IncomingMessageResponseHandler responseCallback) {
//                ByteBuffer buffer = message.getData();
//                byte[] data = new byte[buffer.remaining()];
//                buffer.get(data);
//
//                if (new String(data).equals("HIEVERYBODY! :)")) {
//                    responseCallback.responseReady(new OutgoingResponse("THIS IS THE RESPONSE".getBytes()));
//                } else {
//                    responseCallback.terminate();
//                }
//            }
//        };
//
//        try {
//            transport1.start(EMPTY_INCOMING_FILTER, listener, EMPTY_OUTGOING_FILTER);
//            transport2.start(EMPTY_INCOMING_FILTER, new TerminateIncomingMessageListener<InetSocketAddress>(), EMPTY_OUTGOING_FILTER);
//
//            InetSocketAddress to = new InetSocketAddress("localhost", port1);
//            byte[] data = "HIEVERYBODY! :)".getBytes();
//            OutgoingMessage<InetSocketAddress> outgoingMessage = new OutgoingMessage<>(to, data);
//            IncomingResponse<InetSocketAddress> incomingResponse = TransportHelper.sendAndWait(transport2, outgoingMessage);
//
//            Assert.assertEquals(ByteBuffer.wrap("THIS IS THE RESPONSE".getBytes()), incomingResponse.getData());
//        } finally {
//            transport1.stop();
//            transport2.stop();
//        }
//    }
//
//    @Test
//    public void terminatedUdpTest() throws Throwable {
//        IncomingMessageListener<InetSocketAddress> listener = new IncomingMessageListener<InetSocketAddress>() {
//
//            @Override
//            public void messageArrived(IncomingMessage<InetSocketAddress> message, IncomingMessageResponseHandler responseCallback) {
//                responseCallback.terminate();
//            }
//        };
//
//        try {
//            transport1.start(EMPTY_INCOMING_FILTER, new TerminateIncomingMessageListener<InetSocketAddress>(), EMPTY_OUTGOING_FILTER);
//            transport2.start(EMPTY_INCOMING_FILTER, new TerminateIncomingMessageListener<InetSocketAddress>(), EMPTY_OUTGOING_FILTER);
//
//            InetSocketAddress to = new InetSocketAddress("localhost", port2);
//            byte[] data = "HIEVERYBODY! :)".getBytes();
//            OutgoingMessage<InetSocketAddress> outgoingMessage = new OutgoingMessage<>(to, data);
//            IncomingResponse<InetSocketAddress> incomingResponse = TransportHelper.sendAndWait(transport1, outgoingMessage);
//
//            Assert.assertNull(incomingResponse);
//        } finally {
//            transport1.stop();
//            transport2.stop();
//        }
//    }
//
//    @Test
//    public void noResponseUdpTest() throws Throwable {
//        try {
//            transport2.start(EMPTY_INCOMING_FILTER, new TerminateIncomingMessageListener<InetSocketAddress>(), EMPTY_OUTGOING_FILTER);
//            
//            InetSocketAddress to = new InetSocketAddress("www.microsoft.com", 12345);
//            byte[] data = "HIEVERYBODY! :)".getBytes();
//            OutgoingMessage<InetSocketAddress> outgoingMessage = new OutgoingMessage<>(to, data);
//
//            IncomingResponse<InetSocketAddress> incomingResponse = TransportHelper.sendAndWait(transport2, outgoingMessage);
//
//            Assert.assertNull(incomingResponse);
//        } finally {
//            transport2.stop();
//        }
//    }
}
