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
package com.offbynull.peernetic.demos.transport;

import com.offbynull.peernetic.rpc.transport.transports.test.TestTransportFactory;
import com.offbynull.peernetic.rpc.transport.IncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.IncomingMessageResponseListener;
import com.offbynull.peernetic.rpc.transport.OutgoingMessageResponseListener;
import com.offbynull.peernetic.rpc.transport.Transport;
import com.offbynull.peernetic.rpc.transport.transports.test.PerfectLine;
import com.offbynull.peernetic.rpc.transport.transports.test.TestHub;
import com.offbynull.peernetic.rpc.transport.transports.test.TestTransport;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Benchmarks {@link TestTransport}.
 * @author Kasra Faghihi
 */
public final class TestTransportBenchmark {
    private static final int NUM_OF_TRANSPORTS = 20;
    private static TestHub<Integer> fakeHub = new TestHub<>(new PerfectLine<Integer>());
    private static List<Transport<Integer>> transports = new ArrayList<>();
    
    private TestTransportBenchmark() {
        // do nothing
    }
    
    /**
     * Entry-point.
     * @param args unused
     * @throws Throwable on error
     */
    public static void main(String[] args) throws Throwable {
        fakeHub.start();

        for (int i = 0; i < NUM_OF_TRANSPORTS; i++) {
            TestTransportFactory<Integer> transportFactory = new TestTransportFactory<>(fakeHub, i);
            Transport<Integer> transport = transportFactory.createTransport();
            transport.start(new EchoIncomingMessageListener());

            transports.add(transport);
        }

        for (int i = 0; i < NUM_OF_TRANSPORTS; i++) {
            for (int j = 0; j < NUM_OF_TRANSPORTS; j++) {
                if (i == j) {
                    continue;
                }
                
                issueMessage(i, j);
            }
        }
    }
    
    private static void issueMessage(int from, int to) {
        final long time = System.currentTimeMillis();

        ByteBuffer data = ByteBuffer.allocate(8);
        data.putLong(0, time);

        transports.get(to).sendMessage(from, data, new ReportAndReissueOutgoingMessageResponseListener(from, to));
    }

    private static final class EchoIncomingMessageListener implements IncomingMessageListener<Integer> {

        @Override
        public void messageArrived(Integer from, ByteBuffer message, IncomingMessageResponseListener responseCallback) {
            responseCallback.responseReady(message);
        }
    }

    private static final class ReportAndReissueOutgoingMessageResponseListener
            implements OutgoingMessageResponseListener {
        private static volatile int counter;
        private Integer from;
        private Integer to;

        public ReportAndReissueOutgoingMessageResponseListener(Integer from, Integer to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public void responseArrived(ByteBuffer response) {
            long diff = System.currentTimeMillis() - response.getLong(response.position());
            int count = counter++;
            if (count % 10000 == 0) {
                System.out.println("Response time: " + diff + "(" + count + ")");
            }
            
            issueMessage(from, to);
        }

        @Override
        public void errorOccurred(Object error) {
            System.err.println("ERROR: " + error);
            issueMessage(from, to);
        }
    }
}