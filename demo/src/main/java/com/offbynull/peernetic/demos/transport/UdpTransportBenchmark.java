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

import com.offbynull.peernetic.rpc.UdpTransportFactory;
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
import com.offbynull.peernetic.rpc.transports.udp.UdpTransport;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Benchmarks {@link UdpTransport}.
 * @author Kasra Faghihi
 */
public final class UdpTransportBenchmark {
    private static final int NUM_OF_TRANSPORTS = 20;
    private static Map<InetSocketAddress, Transport<InetSocketAddress>> transports = new HashMap<>();
    
    private UdpTransportBenchmark() {
        // do nothing
    }
    
    /**
     * Entry-point.
     * @param args unused
     * @throws Throwable on error
     */
    public static void main(String[] args) throws Throwable {
        for (int i = 0; i < NUM_OF_TRANSPORTS; i++) {
            final UdpTransportFactory udpTransportFactory = new UdpTransportFactory();
            udpTransportFactory.setListenAddress(new InetSocketAddress(InetAddress.getLocalHost(), 10000 + i));

            InetSocketAddress addr = new InetSocketAddress(InetAddress.getLocalHost(), 10000 + i);
            Transport transport = udpTransportFactory.createTransport();
            transport.start(new CompositeIncomingFilter<>(Collections.<IncomingFilter<Integer>>emptyList()),
                    new EchoIncomingMessageListener(),
                    new CompositeOutgoingFilter<>(Collections.<OutgoingFilter<Integer>>emptyList()));
            transports.put(addr, transport);
        }
        
        for (int i = 0; i < NUM_OF_TRANSPORTS; i++) {
            for (int j = 0; j < NUM_OF_TRANSPORTS; j++) {
                if (i == j) {
                    continue;
                }
                
                InetSocketAddress fromAddr = new InetSocketAddress(InetAddress.getLocalHost(), 10000 + i);
                InetSocketAddress toAddr = new InetSocketAddress(InetAddress.getLocalHost(),
                        10000 + ((i + 1) % NUM_OF_TRANSPORTS)); // NOPMD
                issueMessage(fromAddr, toAddr);
            }
        }
    }
    
    private static void issueMessage(InetSocketAddress from, InetSocketAddress to) {
        final long time = System.currentTimeMillis();

        ByteBuffer data = ByteBuffer.allocate(8);
        data.putLong(0, time);

        OutgoingMessage<InetSocketAddress> message = new OutgoingMessage<>(from, data);
        transports.get(to).sendMessage(message, new ReportAndReissueOutgoingMessageResponseListener(from, to));
    }

    private static final class EchoIncomingMessageListener implements IncomingMessageListener<InetSocketAddress> {

        @Override
        public void messageArrived(IncomingMessage<InetSocketAddress> message, IncomingMessageResponseHandler responseCallback) {
            ByteBuffer data = message.getData();
            responseCallback.responseReady(new OutgoingResponse(data));
        }
    }

    private static final class ReportAndReissueOutgoingMessageResponseListener
            implements OutgoingMessageResponseListener<InetSocketAddress> {
        private InetSocketAddress from;
        private InetSocketAddress to;

        public ReportAndReissueOutgoingMessageResponseListener(InetSocketAddress from, InetSocketAddress to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public void responseArrived(IncomingResponse<InetSocketAddress> response) {
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
