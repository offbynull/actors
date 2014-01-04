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

import com.offbynull.peernetic.rpc.transport.IncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.IncomingMessageResponseListener;
import com.offbynull.peernetic.rpc.transport.TransportUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public final class TestTransportTest {
    
    private static final long TIMEOUT_DURATION = 500;
    private static final int ID_CACHE_SIZE = 1024;

    private Integer address1;
    private Integer address2;
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
        address1 = 1;
        address2 = 2;
        
        hub = new TestHub<>(new PerfectLine<Integer>());
        transport1 = new TestTransport<>(address1, ID_CACHE_SIZE, TIMEOUT_DURATION, TIMEOUT_DURATION, hub);
        transport2 = new TestTransport<>(address2, ID_CACHE_SIZE, TIMEOUT_DURATION, TIMEOUT_DURATION, hub);
        hub.start();
    }

    @After
    public void tearDown() throws IOException {
    }

    @Test
    public void selfUdpTest() throws Throwable {
        IncomingMessageListener<Integer> listener = new IncomingMessageListener<Integer>() {

            @Override
            public void messageArrived(Integer from, ByteBuffer message, IncomingMessageResponseListener responseCallback) {
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

            Integer to = address1;
            byte[] data = "HIEVERYBODY! :)".getBytes();
            ByteBuffer incomingResponse = TransportUtils.<Integer>sendAndWait(transport1, to, ByteBuffer.wrap(data));

            Assert.assertEquals(ByteBuffer.wrap("THIS IS THE RESPONSE".getBytes()), incomingResponse);
        } finally {
            transport1.stop();
        }
    }

    @Test
    public void normalUdpTest() throws Throwable {
        IncomingMessageListener<Integer> listener = new IncomingMessageListener<Integer>() {

            @Override
            public void messageArrived(Integer from, ByteBuffer message, IncomingMessageResponseListener responseCallback) {
                byte[] data = new byte[message.remaining()];
                message.get(data);

                if (new String(data).equals("HIEVERYBODY! :)")) {
                    responseCallback.responseReady(ByteBuffer.wrap("THIS IS THE RESPONSE".getBytes()));
                } else {
                    responseCallback.terminate();
                }
            }
        };
        
        IncomingMessageListener<Integer> termListener = new IncomingMessageListener<Integer>() {

            @Override
            public void messageArrived(Integer from, ByteBuffer message, IncomingMessageResponseListener responseCallback) {
                responseCallback.terminate();
            }
        };

        try {
            transport1.start(listener);
            transport2.start(termListener);

            Integer to = address1;
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
        IncomingMessageListener<Integer> termListener = new IncomingMessageListener<Integer>() {

            @Override
            public void messageArrived(Integer from, ByteBuffer message, IncomingMessageResponseListener responseCallback) {
                responseCallback.terminate();
            }
        };

        try {
            transport1.start(termListener);
            transport2.start(termListener);

            Integer to = address2;
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
        IncomingMessageListener<Integer> listener = new IncomingMessageListener<Integer>() {

            @Override
            public void messageArrived(Integer from, ByteBuffer message, IncomingMessageResponseListener responseCallback) {
                responseCallback.terminate();
            }
        };

        try {
            transport2.start(listener);
            
            Integer to = 3;
            byte[] data = "HIEVERYBODY! :)".getBytes();
            ByteBuffer incomingResponse = TransportUtils.sendAndWait(transport2, to, ByteBuffer.wrap(data));

            Assert.assertNull(incomingResponse);
        } finally {
            transport2.stop();
        }
    }
}
