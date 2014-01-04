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
package com.offbynull.peernetic.rpc.services.nat;

import com.offbynull.peernetic.rpc.RpcInvokeKeys;
import com.offbynull.peernetic.rpc.invoke.InvokeThreadInformation;
import com.offbynull.peernetic.rpc.transport.IncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.IncomingMessageResponseListener;
import com.offbynull.peernetic.rpc.transport.OutgoingMessageResponseListener;
import com.offbynull.peernetic.rpc.transport.Transport;
import com.offbynull.peernetic.rpc.transport.transports.tcp.TcpTransport;
import com.offbynull.peernetic.rpc.transport.transports.udp.UdpTransport;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * NAT helper implementation.
 * @author Kasra Faghihi
 */
public final class NatHelperServiceImplementation implements NatHelperService {
    
    @Override
    public String getAddress() {
        Object from = InvokeThreadInformation.getInfo(RpcInvokeKeys.FROM_ADDRESS);
        
        if (!(from instanceof InetSocketAddress)) {
            return null;
        }
        
        InetSocketAddress inetFrom = (InetSocketAddress) from;
        return inetFrom.getAddress().getHostAddress() + " " + inetFrom.getPort();
    }

    @Override
    public TestPortResult testPort(ConnectionType type, int port, byte[] challenge) {
        Transport<InetSocketAddress> transport = null;
        try {
            if (challenge.length != 8) {
                throw new RuntimeException();
            }

            Object from = InvokeThreadInformation.getInfo(RpcInvokeKeys.FROM_ADDRESS);
            if (!(from instanceof InetSocketAddress)) {
                return TestPortResult.ERROR;
            }
            InetSocketAddress inetFrom = (InetSocketAddress) from;
            
            int selfPort =  10024 + (int) (Math.random() * (65535 - 10024)); // random port from 10024 to 65535

            switch (type) {
                case TCP:
                    transport = new TcpTransport(new InetSocketAddress(selfPort), 1024, 1024, 10000L);
                    break;
                case UDP:
                    transport = new UdpTransport(new InetSocketAddress(selfPort), 1024, 5, 50L, 10000L, 10000L);
                    break;
                default:
                    return TestPortResult.ERROR;
            }
            
            transport.start(new IncomingMessageListener<InetSocketAddress>() {

                @Override
                public void messageArrived(InetSocketAddress from, ByteBuffer message, IncomingMessageResponseListener responseCallback) {
                    responseCallback.terminate();
                }
            });
            
            InetSocketAddress inetTo = new InetSocketAddress(inetFrom.getAddress(), port);
            transport.sendMessage(inetTo, ByteBuffer.wrap(challenge), new OutgoingMessageResponseListener() {

                @Override
                public void responseArrived(ByteBuffer response) {
                    // do nothing
                }

                @Override
                public void errorOccurred(Object error) {
                    // do nothing
                }
            });

            return TestPortResult.SUCCESS; // sendAndForget will close the transport for us once it's finished
        } catch (IOException | RuntimeException e) {
            if (transport != null) {
                try {
                    transport.stop();
                } catch (RuntimeException ex) { // NOPMD
                    // do nothing
                }
            }
            
            return TestPortResult.ERROR;
        }
    }
    
}
