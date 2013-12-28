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

import com.offbynull.peernetic.rpc.FakeTransportFactory;
import com.offbynull.peernetic.rpc.transport.CompositeIncomingFilter;
import com.offbynull.peernetic.rpc.transport.CompositeOutgoingFilter;
import com.offbynull.peernetic.rpc.transport.IncomingFilter;
import com.offbynull.peernetic.rpc.transport.IncomingMessage;
import com.offbynull.peernetic.rpc.transport.IncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.IncomingMessageResponseHandler;
import com.offbynull.peernetic.rpc.transport.IncomingResponse;
import com.offbynull.peernetic.rpc.transport.OutgoingFilter;
import com.offbynull.peernetic.rpc.transport.OutgoingMessage;
import com.offbynull.peernetic.rpc.transport.OutgoingMessageResponseListener;
import com.offbynull.peernetic.rpc.transport.OutgoingResponse;
import com.offbynull.peernetic.rpc.transport.Transport;
import com.offbynull.peernetic.rpc.transports.fake.FakeHub;
import com.offbynull.peernetic.rpc.transports.fake.FakeTransport;
import com.offbynull.peernetic.rpc.transports.fake.PerfectLine;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Benchmarks {@link FakeTransport}.
 * @author Kasra Faghihi
 */
public final class FakeTransportBenchmark {
    private static final int NUM_OF_TRANSPORTS = 20;
    private static FakeHub<Integer> fakeHub = new FakeHub<>(new PerfectLine<Integer>());
    private static List<Transport<Integer>> transports = new ArrayList<>();
    
    private FakeTransportBenchmark() {
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
            FakeTransportFactory<Integer> transportFactory = new FakeTransportFactory<>(fakeHub, i);
            Transport<Integer> transport = transportFactory.createTransport();
            transport.start(new CompositeIncomingFilter<>(Collections.<IncomingFilter<Integer>>emptyList()),
                    new EchoIncomingMessageListener(),
                    new CompositeOutgoingFilter<>(Collections.<OutgoingFilter<Integer>>emptyList()));

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

        OutgoingMessage<Integer> message = new OutgoingMessage(from, data);
        transports.get(to).sendMessage(message, new ReportAndReissueOutgoingMessageResponseListener(from, to));
    }

    private static final class EchoIncomingMessageListener implements IncomingMessageListener<Integer> {

        @Override
        public void messageArrived(IncomingMessage<Integer> message, IncomingMessageResponseHandler responseCallback) {
            ByteBuffer data = message.getData();
            responseCallback.responseReady(new OutgoingResponse(data));
        }
    }

    private static final class ReportAndReissueOutgoingMessageResponseListener implements OutgoingMessageResponseListener<Integer> {
        private int from;
        private int to;

        public ReportAndReissueOutgoingMessageResponseListener(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public void responseArrived(IncomingResponse<Integer> response) {
            long diff = System.currentTimeMillis() - response.getData().getLong(0);
            System.out.println("Response time: " + diff);
            
            issueMessage(from, to);
        }

        @Override
        public void internalErrorOccurred(Throwable error) {
            System.err.println("ERROR: " + error);
            
            issueMessage(from, to);
        }

        @Override
        public void timedOut() {
            System.err.println("TIMEDOUT");
            
            issueMessage(from, to);
        }
    }
}
