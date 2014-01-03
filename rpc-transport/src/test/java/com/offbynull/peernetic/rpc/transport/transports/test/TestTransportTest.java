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
package com.offbynull.peernetic.rpc.transport.transports.test;

import com.offbynull.peernetic.common.concurrent.actor.ActorQueue;
import com.offbynull.peernetic.common.concurrent.actor.Message;
import com.offbynull.peernetic.rpc.transport.DropResponseCommand;
import com.offbynull.peernetic.rpc.transport.SendRequestCommand;
import com.offbynull.peernetic.rpc.transport.SendResponseCommand;
import com.offbynull.peernetic.rpc.transport.RequestArrivedEvent;
import com.offbynull.peernetic.rpc.transport.ResponseArrivedEvent;
import com.offbynull.peernetic.rpc.transport.ResponseErroredEvent;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public final class TestTransportTest {
    
    private static final long TIMEOUT_DURATION = 500;
    private static final int ID_CACHE_SIZE = 1024;

    private TestHub<Integer> hub;
    private TestTransport<Integer> transport1;
    private TestTransport<Integer> transport2;

    public TestTransportTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws IOException {
        hub = new TestHub<>(new PerfectLine<Integer>());
        transport1 = new TestTransport(1, ID_CACHE_SIZE, TIMEOUT_DURATION, TIMEOUT_DURATION, hub.getWriter());
        transport2 = new TestTransport(2, ID_CACHE_SIZE, TIMEOUT_DURATION, TIMEOUT_DURATION, hub.getWriter());
        hub.start();
    }

    @After
    public void tearDown() throws IOException {
        hub.stop();
    }

    @Test
    public void selfTestTest() throws Throwable {
        ActorQueue fakeQueue = new ActorQueue();
                    
        Integer to = 1;
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
        
        msgIt = fakeQueue.getReader().pull(1000L);
        Assert.assertFalse(msgIt.hasNext());
    }

    @Test
    public void normalTestTest() throws Throwable {
        ActorQueue fakeQueue = new ActorQueue();
                    
        Integer transport1To = 1;
        Integer transport2To = 2;
        byte[] reqData = "HIEVERYBODY! :)".getBytes();
        byte[] respData = "THIS IS THE RESPONSE".getBytes();
        Message msg;
        Iterator<Message> msgIt;

        try {
            transport1.setDestinationWriter(fakeQueue.getWriter());
            transport1.start();
            
            transport2.setDestinationWriter(fakeQueue.getWriter());
            transport2.start();
            
            
            // write message to transport2 from transport1
            msg = Message.createRespondableMessage(0, fakeQueue.getWriter(),
                    new SendRequestCommand<>(transport2To, ByteBuffer.wrap(reqData)));
            transport1.getWriter().push(msg);
            
            // wait for msg to arrive on transport2
            msgIt = fakeQueue.getReader().pull(2000L);
            Assert.assertTrue(msgIt.hasNext());
            
            msg = msgIt.next();
            RequestArrivedEvent<InetSocketAddress> requestArrivedEvent = (RequestArrivedEvent<InetSocketAddress>) msg.getContent();
            
            Assert.assertEquals(ByteBuffer.wrap(reqData), requestArrivedEvent.getData());
            Assert.assertEquals(transport1To, requestArrivedEvent.getFrom());
            
            // respond back to transport1 from transport2
            msg.getResponder().respondImmediately(new SendResponseCommand<>(ByteBuffer.wrap(respData)));
            
            // wait for transport1 to get resp
            msgIt = fakeQueue.getReader().pull(2000L);
            Assert.assertTrue(msgIt.hasNext());

            msg = msgIt.next();
            ResponseArrivedEvent<InetSocketAddress> responseArrivedEvent = (ResponseArrivedEvent<InetSocketAddress>) msg.getContent();
            
            Assert.assertEquals(ByteBuffer.wrap(respData), responseArrivedEvent.getData());
            Assert.assertEquals(transport2To, responseArrivedEvent.getFrom());
        } finally {
            transport1.stop();
            transport2.stop();
        }
        
        msgIt = fakeQueue.getReader().pull(1000L);
        Assert.assertFalse(msgIt.hasNext());
    }

    @Test
    public void terminatedTestTest() throws Throwable {
        ActorQueue fakeQueue = new ActorQueue();
                    
        Integer transport1To = 1;
        Integer transport2To = 2;
        byte[] reqData = "HIEVERYBODY! :)".getBytes();
        Message msg;
        Iterator<Message> msgIt;

        try {
            transport1.setDestinationWriter(fakeQueue.getWriter());
            transport1.start();
            
            transport2.setDestinationWriter(fakeQueue.getWriter());
            transport2.start();
            
            
            // write message to transport2 from transport1
            msg = Message.createRespondableMessage(0, fakeQueue.getWriter(),
                    new SendRequestCommand<>(transport2To, ByteBuffer.wrap(reqData)));
            transport1.getWriter().push(msg);
            
            // wait for msg to arrive on transport2
            msgIt = fakeQueue.getReader().pull(2000L);
            Assert.assertTrue(msgIt.hasNext());
            
            msg = msgIt.next();
            RequestArrivedEvent<InetSocketAddress> requestArrivedEvent = (RequestArrivedEvent<InetSocketAddress>) msg.getContent();
            
            Assert.assertEquals(ByteBuffer.wrap(reqData), requestArrivedEvent.getData());
            Assert.assertEquals(transport1To, requestArrivedEvent.getFrom());
            
            // respond back to transport1 from transport2
            msg.getResponder().respondImmediately(new DropResponseCommand());
            
            // wait for transport1 to get resp
            msgIt = fakeQueue.getReader().pull(2000L);
            Assert.assertTrue(msgIt.hasNext());

            msg = msgIt.next();
            ResponseErroredEvent responseArrivedEvent = (ResponseErroredEvent) msg.getContent();
            
            Assert.assertEquals(0, msg.getResponseToId());
        } finally {
            transport1.stop();
            transport2.stop();
        }
        
        msgIt = fakeQueue.getReader().pull(1000L);
        Assert.assertFalse(msgIt.hasNext());
    }

    @Test
    public void noResponseTestTest() throws Throwable {
        ActorQueue fakeQueue = new ActorQueue();
                    
        Integer transport1To = 1;
        Integer transport2To = 2;
        byte[] reqData = "HIEVERYBODY! :)".getBytes();
        Message msg;
        Iterator<Message> msgIt;

        try {
            transport1.setDestinationWriter(fakeQueue.getWriter());
            transport1.start();
            
            transport2.setDestinationWriter(fakeQueue.getWriter());
            transport2.start();
            
            
            // write message to transport2 from transport1
            msg = Message.createRespondableMessage(0, fakeQueue.getWriter(),
                    new SendRequestCommand<>(transport2To, ByteBuffer.wrap(reqData)));
            transport1.getWriter().push(msg);
            
            // wait for msg to arrive on transport2
            msgIt = fakeQueue.getReader().pull(2000L);
            Assert.assertTrue(msgIt.hasNext());
            
            msg = msgIt.next();
            RequestArrivedEvent<InetSocketAddress> requestArrivedEvent = (RequestArrivedEvent<InetSocketAddress>) msg.getContent();
            
            Assert.assertEquals(ByteBuffer.wrap(reqData), requestArrivedEvent.getData());
            Assert.assertEquals(transport1To, requestArrivedEvent.getFrom());
            
            // don't bother responding back to transport1 from transport2
            //msg.getResponder().respondImmediately(new DropResponseCommand());
            
            // wait for transport1 to get resp
            msgIt = fakeQueue.getReader().pull(2000L);
            Assert.assertTrue(msgIt.hasNext());

            msg = msgIt.next();
            ResponseErroredEvent responseArrivedEvent = (ResponseErroredEvent) msg.getContent();
            
            Assert.assertEquals(0, msg.getResponseToId());
        } finally {
            transport1.stop();
            transport2.stop();
        }
        
        msgIt = fakeQueue.getReader().pull(1000L);
        Assert.assertFalse(msgIt.hasNext());
    }
}
