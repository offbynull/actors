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

import com.offbynull.peernetic.rpc.transport.IncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.IncomingMessageResponseListener;
import com.offbynull.peernetic.rpc.transport.TransportUtils;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public final class TcpTransportTest {
    
    private static final long TIMEOUT_DURATION = 500;
    private static final int BUFFER_SIZE = 65535;

    private TcpTransport transport1;
    private TcpTransport transport2;
    private InetSocketAddress address1;
    private InetSocketAddress address2;
    private static AtomicInteger nextPort;

    public TcpTransportTest() {
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
        address1 = new InetSocketAddress("localhost", nextPort.getAndIncrement());
        transport1 = new TcpTransport(address1, BUFFER_SIZE, BUFFER_SIZE, TIMEOUT_DURATION);
        
        address2 = new InetSocketAddress("localhost", nextPort.getAndIncrement());
        transport2 = new TcpTransport(address2, BUFFER_SIZE, BUFFER_SIZE, TIMEOUT_DURATION);
    }

    @After
    public void tearDown() throws IOException {
    }

    @Test
    public void selfUdpTest() throws Throwable {
        IncomingMessageListener<InetSocketAddress> listener = new IncomingMessageListener<InetSocketAddress>() {

            @Override
            public void messageArrived(InetSocketAddress from, ByteBuffer message, IncomingMessageResponseListener responseCallback) {
                byte[] data = new byte[message.remaining()];
                message.get(data);

                if (new String(data).equals("HIEVERYBODY! :)")) {
                    responseCallback.responseReady(ByteBuffer.wrap("THIS IS THE RESPONSE".getBytes()));
                } else {
                    responseCallback.terminate();
                }
            }
        };

        try {
            transport1.start(listener);

            InetSocketAddress to = address1;
            byte[] data = "HIEVERYBODY! :)".getBytes();
            ByteBuffer incomingResponse = TransportUtils.<InetSocketAddress>sendAndWait(transport1, to, ByteBuffer.wrap(data));

            Assert.assertEquals(ByteBuffer.wrap("THIS IS THE RESPONSE".getBytes()), incomingResponse);
        } finally {
            transport1.stop();
        }
    }

    @Test
    public void normalUdpTest() throws Throwable {
        IncomingMessageListener<InetSocketAddress> listener = new IncomingMessageListener<InetSocketAddress>() {

            @Override
            public void messageArrived(InetSocketAddress from, ByteBuffer message, IncomingMessageResponseListener responseCallback) {
                byte[] data = new byte[message.remaining()];
                message.get(data);

                if (new String(data).equals("HIEVERYBODY! :)")) {
                    responseCallback.responseReady(ByteBuffer.wrap("THIS IS THE RESPONSE".getBytes()));
                } else {
                    responseCallback.terminate();
                }
            }
        };
        
        IncomingMessageListener<InetSocketAddress> termListener = new IncomingMessageListener<InetSocketAddress>() {

            @Override
            public void messageArrived(InetSocketAddress from, ByteBuffer message, IncomingMessageResponseListener responseCallback) {
                responseCallback.terminate();
            }
        };

        try {
            transport1.start(listener);
            transport2.start(termListener);

            InetSocketAddress to = address1;
            byte[] data = "HIEVERYBODY! :)".getBytes();
            ByteBuffer incomingResponse = TransportUtils.sendAndWait(transport2, to, ByteBuffer.wrap(data));

            Assert.assertEquals(ByteBuffer.wrap("THIS IS THE RESPONSE".getBytes()), incomingResponse);
        } finally {
            transport1.stop();
            transport2.stop();
        }
    }

    @Test
    public void terminatedUdpTest() throws Throwable {
        IncomingMessageListener<InetSocketAddress> termListener = new IncomingMessageListener<InetSocketAddress>() {

            @Override
            public void messageArrived(InetSocketAddress from, ByteBuffer message, IncomingMessageResponseListener responseCallback) {
                responseCallback.terminate();
            }
        };

        try {
            transport1.start(termListener);
            transport2.start(termListener);

            InetSocketAddress to = address2;
            byte[] data = "HIEVERYBODY! :)".getBytes();
            ByteBuffer incomingResponse = TransportUtils.sendAndWait(transport1, to, ByteBuffer.wrap(data));

            Assert.assertNull(incomingResponse);
        } finally {
            transport1.stop();
            transport2.stop();
        }
    }

    @Test
    public void noResponseUdpTest() throws Throwable {
        IncomingMessageListener<InetSocketAddress> listener = new IncomingMessageListener<InetSocketAddress>() {

            @Override
            public void messageArrived(InetSocketAddress from, ByteBuffer message, IncomingMessageResponseListener responseCallback) {
                responseCallback.terminate();
            }
        };

        try {
            transport2.start(listener);
            
            InetSocketAddress to = new InetSocketAddress("www.microsoft.com", 12345);
            byte[] data = "HIEVERYBODY! :)".getBytes();
            ByteBuffer incomingResponse = TransportUtils.sendAndWait(transport2, to, ByteBuffer.wrap(data));

            Assert.assertNull(incomingResponse);
        } finally {
            transport2.stop();
        }
    }
}