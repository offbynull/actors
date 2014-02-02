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
package com.offbynull.peernetic.router.pcp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

public final class PcpReceiver {
    private InetAddress gatewayAddress;
    private AtomicReference<MulticastSocket> currentSocket = new AtomicReference<>();

    public PcpReceiver(InetAddress gatewayAddress) {
        Validate.notNull(gatewayAddress);
        
        this.gatewayAddress = gatewayAddress;
    }
    
    public void start(PcpEventListener listener) throws IOException {
        Validate.notNull(listener);

        MulticastSocket socket = null;
        try {
            final InetAddress ipv4Group = InetAddress.getByName("224.0.0.1"); // NOPMD
            final InetAddress ipv6Group = InetAddress.getByName("ff02::1"); // NOPMD
            final int port = 5350;
            final InetSocketAddress ipv4GroupAddress = new InetSocketAddress(ipv4Group, port);
            final InetSocketAddress ipv6GroupAddress = new InetSocketAddress(ipv6Group, port);

            socket = new MulticastSocket(port);
            
            if (!currentSocket.compareAndSet(null, socket)) {
                IOUtils.closeQuietly(socket);
                return;
            }
            
            socket.setReuseAddress(true);
            
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addrs = networkInterface.getInetAddresses();
                while (addrs.hasMoreElements()) { // make sure atleast 1 ipv4 addr bound to interface
                    InetAddress addr = addrs.nextElement();
                    
                    try {
                        if (addr instanceof Inet4Address) {
                            socket.joinGroup(ipv4GroupAddress, networkInterface);
                        } else if (addr instanceof Inet6Address) {
                            socket.joinGroup(ipv6GroupAddress, networkInterface);
                        }
                    } catch (IOException ioe) { // occurs with certain interfaces
                        // do nothing
                    }
                }
            }

            ByteBuffer buffer = ByteBuffer.allocate(1100);
            DatagramPacket data = new DatagramPacket(buffer.array(), buffer.capacity());

            while (true) {
                buffer.clear();
                socket.receive(data);
                buffer.limit(data.getLength());
                
                if (!data.getAddress().equals(gatewayAddress)) { // data isn't from our gateway, ignore
                    continue;
                }
                
                AnnouncePcpResponse response;
                try {
                    response = new AnnouncePcpResponse(buffer);
                } catch (Exception e) {
                    continue;
                }
                
                listener.incomingEvent(response);
            }
        } catch (IOException ioe) {
            if (currentSocket.get() == null) {
                return; // caused by stop();
            }
            
            throw ioe;
        } finally {
            IOUtils.closeQuietly(socket);
            currentSocket.set(null);
        }
    }
    
    /**
     * Stop listening.
     */
    public void stop() {
        MulticastSocket socket = currentSocket.getAndSet(null);
        if (socket != null) {
            IOUtils.closeQuietly(socket);
        }
    }
}
