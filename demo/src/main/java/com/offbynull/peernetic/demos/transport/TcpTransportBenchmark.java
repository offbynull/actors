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

import com.offbynull.peernetic.rpc.transport.transports.tcp.TcpTransportFactory;
import com.offbynull.peernetic.rpc.transport.IncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.IncomingMessageResponseListener;
import com.offbynull.peernetic.rpc.transport.OutgoingMessageResponseListener;
import com.offbynull.peernetic.rpc.transport.Transport;
import com.offbynull.peernetic.rpc.transport.transports.tcp.TcpTransport;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Benchmarks {@link TcpTransport}.
 * <p/>
 * Has issues with TIME_WAIT due to hammering out new connections as fast as possible. Also, if on Windows, check out the following links:
 * <ul>
 * <li>http://stackoverflow.com/questions/10088363/java-net-socketexception-no-buffer-space-available-maximum-connections-reached</li>
 * <li>http://support.microsoft.com/kb/2577795</li>
 * </ul>
 * @author Kasra Faghihi
 */
public final class TcpTransportBenchmark {
    private static final int NUM_OF_TRANSPORTS = 50;
    private static Map<InetSocketAddress, Transport<InetSocketAddress>> transports = new HashMap<>();
    
    private TcpTransportBenchmark() {
        // do nothing
    }
    
    /**
     * Entry-point.
     * @param args unused
     * @throws Throwable on error
     */
    public static void main(String[] args) throws Throwable {
        for (int i = 0; i < NUM_OF_TRANSPORTS; i++) {
            final TcpTransportFactory tcpTransportFactory = new TcpTransportFactory();
            tcpTransportFactory.setListenAddress(new InetSocketAddress(InetAddress.getLocalHost(), 10000 + i));

            InetSocketAddress addr = new InetSocketAddress(InetAddress.getLocalHost(), 10000 + i);
            Transport transport = tcpTransportFactory.createTransport();
            transport.start(new EchoIncomingMessageListener());
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
        
        while (true) {
            Thread.sleep(1000L);
        }
    }
    
    private static void issueMessage(InetSocketAddress from, InetSocketAddress to) {
        final long time = System.currentTimeMillis();

        ByteBuffer data = ByteBuffer.allocate(8);
        data.putLong(0, time);

        transports.get(to).sendMessage(from, data, new ReportAndReissueOutgoingMessageResponseListener(from, to));
    }

    private static final class EchoIncomingMessageListener implements IncomingMessageListener<InetSocketAddress> {

        @Override
        public void messageArrived(InetSocketAddress from, ByteBuffer message, IncomingMessageResponseListener responseCallback) {
            responseCallback.responseReady(message);
        }
    }

    private static final class ReportAndReissueOutgoingMessageResponseListener
            implements OutgoingMessageResponseListener {
        private static volatile int counter;
        private InetSocketAddress from;
        private InetSocketAddress to;

        public ReportAndReissueOutgoingMessageResponseListener(InetSocketAddress from, InetSocketAddress to) {
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
