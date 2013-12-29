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
import com.offbynull.peernetic.rpc.transport.CompositeIncomingFilter;
import com.offbynull.peernetic.rpc.transport.CompositeOutgoingFilter;
import com.offbynull.peernetic.rpc.transport.IncomingFilter;
import com.offbynull.peernetic.rpc.transport.OutgoingFilter;
import com.offbynull.peernetic.rpc.transport.OutgoingMessage;
import com.offbynull.peernetic.rpc.transport.TerminateIncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.Transport;
import com.offbynull.peernetic.rpc.transport.TransportHelper;
import com.offbynull.peernetic.rpc.transport.transports.tcp.TcpTransport;
import com.offbynull.peernetic.rpc.transport.transports.udp.UdpTransport;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;

/**
 * NAT helper implementation.
 * @author Kasra Faghihi
 */
public final class NatHelperServiceImplementation implements NatHelperService {

    private static final IncomingFilter<InetSocketAddress> EMPTY_INCOMING_FILTER =
            new CompositeIncomingFilter<>(Collections.<IncomingFilter<InetSocketAddress>>emptyList());
    private static final OutgoingFilter<InetSocketAddress> EMPTY_OUTGOING_FILTER =
            new CompositeOutgoingFilter<>(Collections.<OutgoingFilter<InetSocketAddress>>emptyList());
    
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
        Transport transport = null;
        try {
            if (challenge.length != 8) {
                throw new RuntimeException();
            }

            Object from = InvokeThreadInformation.getInfo(RpcInvokeKeys.FROM_ADDRESS);
            if (!(from instanceof InetSocketAddress)) {
                return TestPortResult.ERROR;
            }
            InetSocketAddress inetFrom = (InetSocketAddress) from;

            switch (type) {
                case TCP:
                    transport = new TcpTransport(0, 1024, 1024, 10000L);
                    break;
                case UDP:
                    transport = new UdpTransport(0, 1024, 1024, 10000L);
                    break;
                default:
                    return TestPortResult.ERROR;
            }
            
            transport.start(EMPTY_INCOMING_FILTER, new TerminateIncomingMessageListener<InetSocketAddress>(), EMPTY_OUTGOING_FILTER);
            
            InetSocketAddress inetTo = new InetSocketAddress(inetFrom.getAddress(), port);
            OutgoingMessage<InetSocketAddress> outgoingMessage = new OutgoingMessage<>(inetTo, challenge);
            TransportHelper.sendAndForget(transport, outgoingMessage);

            return TestPortResult.SUCCESS; // sendAndForget will close the transport for us once it's finished
        } catch (IOException | RuntimeException e) {
            if (transport != null) {
                try {
                    transport.stop();
                } catch (IOException | RuntimeException ex) { // NOPMD
                    // do nothing
                }
            }
            
            return TestPortResult.ERROR;
        }
    }
    
}
