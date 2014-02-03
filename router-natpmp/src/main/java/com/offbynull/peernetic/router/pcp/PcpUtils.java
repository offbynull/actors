package com.offbynull.peernetic.router.pcp;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.channels.MulticastChannel;
import java.util.Enumeration;

public final class PcpUtils {
    private PcpUtils() {
        // do nothing
    }
    
    static final byte[] convertToIpv6Array(InetAddress address) {
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
    
    static final void multicastListenOnAllIpv4InterfaceAddresses(MulticastChannel channel) throws IOException {
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
                } catch (IOException ioe) { // occurs with certain interfaces
                    // do nothing
                }
            }
        }
    }

    static final void multicastListenOnAllIpv6InterfaceAddresses(MulticastChannel channel) throws IOException {
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
                } catch (IOException ioe) { // occurs with certain interfaces
                    // do nothing
                }
            }
        }
    }
}
