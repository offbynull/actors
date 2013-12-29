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
package com.offbynull.peernetic.rpc.transport.transports.tcp;

import com.offbynull.peernetic.rpc.transport.CompositeIncomingFilter;
import com.offbynull.peernetic.rpc.transport.CompositeOutgoingFilter;
import com.offbynull.peernetic.rpc.transport.IncomingFilter;
import com.offbynull.peernetic.rpc.transport.IncomingMessage;
import com.offbynull.peernetic.rpc.transport.IncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.IncomingMessageResponseHandler;
import com.offbynull.peernetic.rpc.transport.IncomingResponse;
import com.offbynull.peernetic.rpc.transport.OutgoingFilter;
import com.offbynull.peernetic.rpc.transport.OutgoingMessage;
import com.offbynull.peernetic.rpc.transport.OutgoingResponse;
import com.offbynull.peernetic.rpc.transport.TerminateIncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.TransportHelper;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public final class TcpTransportTest {

    private static final IncomingFilter<InetSocketAddress> EMPTY_INCOMING_FILTER =
            new CompositeIncomingFilter<>(Collections.<IncomingFilter<InetSocketAddress>>emptyList());
    private static final OutgoingFilter<InetSocketAddress> EMPTY_OUTGOING_FILTER =
            new CompositeOutgoingFilter<>(Collections.<OutgoingFilter<InetSocketAddress>>emptyList());
    
    private static final long TIMEOUT_DURATION = 500;
    private static final int BUFFER_SIZE = 500;

    private TcpTransport transport1;
    private TcpTransport transport2;
    private int port1;
    private int port2;
    private static AtomicInteger nextPort;

    public TcpTransportTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        nextPort = new AtomicInteger(13000);
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws IOException {
        port1 = nextPort.getAndIncrement();
        transport1 = new TcpTransport(port1, BUFFER_SIZE, BUFFER_SIZE, TIMEOUT_DURATION);
        
        port2 = nextPort.getAndIncrement();
        transport2 = new TcpTransport(port2, BUFFER_SIZE, BUFFER_SIZE, TIMEOUT_DURATION);
    }

    @After
    public void tearDown() throws IOException {
    }

    @Test
    public void selfTcpTest() throws Throwable {
        IncomingMessageListener<InetSocketAddress> listener = new IncomingMessageListener<InetSocketAddress>() {

            @Override
            public void messageArrived(IncomingMessage<InetSocketAddress> message, IncomingMessageResponseHandler responseCallback) {
                ByteBuffer buffer = message.getData();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);

                if (new String(data).equals("HIEVERYBODY! :)")) {
                    responseCallback.responseReady(new OutgoingResponse("THIS IS THE RESPONSE".getBytes()));
                } else {
                    responseCallback.terminate();
                }
            }
        };

        try {
            transport1.start(EMPTY_INCOMING_FILTER, listener, EMPTY_OUTGOING_FILTER);

            InetSocketAddress to = new InetSocketAddress("localhost", port1);
            byte[] data = "HIEVERYBODY! :)".getBytes();
            OutgoingMessage<InetSocketAddress> outgoingMessage = new OutgoingMessage<>(to, data);
            IncomingResponse<InetSocketAddress> incomingResponse = TransportHelper.sendAndWait(transport1, outgoingMessage);

            Assert.assertEquals(ByteBuffer.wrap("THIS IS THE RESPONSE".getBytes()), incomingResponse.getData());
        } finally {
            transport1.stop();
        }
    }

    @Test
    public void normalTcpTest() throws Throwable {
        IncomingMessageListener<InetSocketAddress> listener = new IncomingMessageListener<InetSocketAddress>() {

            @Override
            public void messageArrived(IncomingMessage<InetSocketAddress> message, IncomingMessageResponseHandler responseCallback) {
                ByteBuffer buffer = message.getData();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);

                if (new String(data).equals("HIEVERYBODY! :)")) {
                    responseCallback.responseReady(new OutgoingResponse("THIS IS THE RESPONSE".getBytes()));
                } else {
                    responseCallback.terminate();
                }
            }
        };

        try {
            transport1.start(EMPTY_INCOMING_FILTER, listener, EMPTY_OUTGOING_FILTER);
            transport2.start(EMPTY_INCOMING_FILTER, new TerminateIncomingMessageListener<InetSocketAddress>(), EMPTY_OUTGOING_FILTER);

            InetSocketAddress to = new InetSocketAddress("localhost", port1);
            byte[] data = "HIEVERYBODY! :)".getBytes();
            OutgoingMessage<InetSocketAddress> outgoingMessage = new OutgoingMessage<>(to, data);
            IncomingResponse<InetSocketAddress> incomingResponse = TransportHelper.sendAndWait(transport2, outgoingMessage);

            Assert.assertEquals(ByteBuffer.wrap("THIS IS THE RESPONSE".getBytes()), incomingResponse.getData());
        } finally {
            transport1.stop();
            transport2.stop();
        }
    }

    @Test
    public void terminatedTcpTest() throws Throwable {
        try {
            transport1.start(EMPTY_INCOMING_FILTER, new TerminateIncomingMessageListener<InetSocketAddress>(), EMPTY_OUTGOING_FILTER);
            transport2.start(EMPTY_INCOMING_FILTER, new TerminateIncomingMessageListener<InetSocketAddress>(), EMPTY_OUTGOING_FILTER);

            InetSocketAddress to = new InetSocketAddress("localhost", port2);
            byte[] data = "HIEVERYBODY! :)".getBytes();
            OutgoingMessage<InetSocketAddress> outgoingMessage = new OutgoingMessage<>(to, data);
            IncomingResponse<InetSocketAddress> incomingResponse = TransportHelper.sendAndWait(transport1, outgoingMessage);

            Assert.assertNull(incomingResponse);
        } finally {
            transport1.stop();
            transport2.stop();
        }
    }

    @Test
    public void noResponseTcpTest() throws Throwable {
        try {
            transport2.start(EMPTY_INCOMING_FILTER, new TerminateIncomingMessageListener<InetSocketAddress>(), EMPTY_OUTGOING_FILTER);
            InetSocketAddress to = new InetSocketAddress("www.microsoft.com", 12345);
            byte[] data = "HIEVERYBODY! :)".getBytes();
            OutgoingMessage<InetSocketAddress> outgoingMessage = new OutgoingMessage<>(to, data);

            IncomingResponse<InetSocketAddress> incomingResponse = TransportHelper.sendAndWait(transport2, outgoingMessage);

            Assert.assertNull(incomingResponse);
        } finally {
            transport2.stop();
        }
    }
}