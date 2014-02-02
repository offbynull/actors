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
    private AtomicReference<Thread> currentThread = new AtomicReference<>();

    public PcpReceiver(InetAddress gatewayAddress) {
        Validate.notNull(gatewayAddress);
        
        this.gatewayAddress = gatewayAddress;
    }
    
    public void start(PcpEventListener listener) throws IOException {
        Validate.notNull(listener);
        
        if (!currentThread.compareAndSet(null, Thread.currentThread())) {
            throw new IllegalStateException("Already running");
        }

        MulticastSocket socket = null;
        try {
            final InetAddress group = InetAddress.getByName("224.0.0.1"); // NOPMD
            final int port = 5350;
            final InetSocketAddress groupAddress = new InetSocketAddress(group, port);

            socket = new MulticastSocket(port);
            socket.setReuseAddress(true);
            
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addrs = networkInterface.getInetAddresses();
                while (addrs.hasMoreElements()) { // make sure atleast 1 ipv4 addr bound to interface
                    if (addrs.nextElement() instanceof Inet4Address) {
                        socket.joinGroup(groupAddress, networkInterface);
                        break;
                    }
                }
            }

            ByteBuffer buffer = ByteBuffer.allocate(1100);
            DatagramPacket data = new DatagramPacket(buffer.array(), buffer.capacity());

            while (true) {
                buffer.clear();
                socket.receive(data);
                buffer.position(data.getLength());
                buffer.flip();
                
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
            if (currentThread.get() == null) {
                return; // ioexception caused by interruption/stop, so just return without propogating error up
            }
            
            throw ioe;
        } finally {
            IOUtils.closeQuietly(socket);
            currentThread.set(null);
        }
    }
    
    /**
     * Stop listening.
     */
    public void stop() {
        Thread oldThread = currentThread.getAndSet(null);
        if (oldThread != null) {
            oldThread.interrupt();
        }
    }
}
