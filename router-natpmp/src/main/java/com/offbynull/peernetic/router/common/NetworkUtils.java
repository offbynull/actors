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
package com.offbynull.peernetic.router.common;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.channels.MulticastChannel;
import java.util.Enumeration;
import org.apache.commons.lang3.Validate;

public final class NetworkUtils {
    private NetworkUtils() {
        // do nothing
    }
    
    /**
     * Convert a IP address to a IPv6 address and dump as a byte array. Essentially, if the input is IPv4 it'll be converted to an
     * IPv4-to-IPv6 address. Otherwise, the IPv6 address will be dumped as-is.
     * @param address address to convert to a ipv6 byte array
     * @return ipv6 byte array
     * @throws NullPointerException if any argument is {@code null}
     */
    public static byte[] convertToIpv6Array(InetAddress address) {
        Validate.notNull(address);
        
        byte[] addrArr = address.getAddress();
        switch (addrArr.length) {
            case 4: {
                // convert ipv4 address to ipv4-mapped ipv6 address
                byte[] newAddrArr = new byte[16];
                newAddrArr[10] = (byte) 0xff;
                newAddrArr[11] = (byte) 0xff;
                System.arraycopy(addrArr, 0, newAddrArr, 12, 4);
                
                return newAddrArr;
            }
            case 16: {
                return addrArr;
            }
            default:
                throw new IllegalStateException();
        }
    }
    
    /**
     * Set a {@link MulticastChannel} to listen on all IPv4 interfaces.
     * @param channel multicast channel to listen on
     * @throws IOException if there's an error
     * @throws NullPointerException if any argument is {@code null}
     */
    public static void multicastListenOnAllIpv4InterfaceAddresses(MulticastChannel channel) throws IOException {
        Validate.notNull(channel);
        
        final InetAddress ipv4Group = InetAddress.getByName("224.0.0.1"); // NOPMD

        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            Enumeration<InetAddress> addrs = networkInterface.getInetAddresses();
            while (addrs.hasMoreElements()) { // make sure atleast 1 ipv4 addr bound to interface
                InetAddress addr = addrs.nextElement();

                try {
                    if (addr instanceof Inet4Address) {
                        channel.join(ipv4Group, networkInterface);
                    }
                } catch (IOException ioe) { // NOPMD
                    // occurs with certain interfaces
                    // do nothing
                }
            }
        }
    }

    /**
     * Set a {@link MulticastChannel} to listen on all IPv6 interfaces.
     * @param channel multicast channel to listen on
     * @throws IOException if there's an error
     * @throws NullPointerException if any argument is {@code null}
     */
    public static void multicastListenOnAllIpv6InterfaceAddresses(MulticastChannel channel) throws IOException {
        Validate.notNull(channel);
        
        final InetAddress ipv6Group = InetAddress.getByName("ff02::1"); // NOPMD

        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            Enumeration<InetAddress> addrs = networkInterface.getInetAddresses();
            while (addrs.hasMoreElements()) { // make sure atleast 1 ipv4 addr bound to interface
                InetAddress addr = addrs.nextElement();

                try {
                    if (addr instanceof Inet6Address) {
                        channel.join(ipv6Group, networkInterface);
                    }
                } catch (IOException ioe) { // NOPMD
                    // occurs with certain interfaces, do nothing
                }
            }
        }
    }
    
    /**
     * Get timeout duration for a NAT-PMP/PCP request as defined by the NAT-PMP/PCP RFC.
     * @param attempt attempt number (e.g. first attempt, second attempt, etc..)
     * @return number of seconds to wait for a response
     * @throws IllegalArgumentException if {@code attempt < 1 || > 9}
     */
    public static long getNatPmpWaitTime(int attempt) {
        Validate.inclusiveBetween(1, 9, attempt);
        
        // timeout duration should double each iteration, starting from 250 according to spec
        // i = 1, maxWaitTime = (1 << (1-1)) * 250 = (1 << 0) * 250 = 1 * 250 = 250
        // i = 2, maxWaitTime = (1 << (2-1)) * 250 = (1 << 1) * 250 = 2 * 250 = 500
        // i = 3, maxWaitTime = (1 << (3-1)) * 250 = (1 << 2) * 250 = 4 * 250 = 1000
        // i = 4, maxWaitTime = (1 << (4-1)) * 250 = (1 << 3) * 250 = 8 * 250 = 2000
        // ...
        return (1 << (attempt - 1)) * 250; // NOPMD
    }
}
