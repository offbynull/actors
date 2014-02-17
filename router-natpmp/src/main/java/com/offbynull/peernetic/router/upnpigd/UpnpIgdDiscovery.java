package com.offbynull.peernetic.router.upnpigd;

import com.offbynull.peernetic.common.utils.ByteBufferUtils;
import com.offbynull.peernetic.router.common.NetworkUtils;
import com.offbynull.peernetic.router.common.UdpCommunicator;
import com.offbynull.peernetic.router.common.UdpCommunicatorListener;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpnpIgdDiscovery {
    private static final int MAX_WAIT = 3;
    private static final Pattern LOCATION_PATTERN = Pattern.compile("(?i)LOCATION:\\s*(.*)\\s*");
    private static final Pattern SERVER_PATTERN = Pattern.compile("(?i)SERVER:\\s*(.*)\\s*");
    private static final String IPV4_SEARCH_QUERY = "M-SEARCH * HTTP/1.1\r\n"
            + "HOST: 239.255.255.250:1900\r\n"
            + "MAN: ssdp:discover\r\n"
            + "MX: " + MAX_WAIT + "\r\n" // server should send response in rand(0, MX)
            + "ST: ssdp:all\r\n"
            + "\r\n";
    private static final String IPV6_SEARCH_QUERY = "M-SEARCH * HTTP/1.1\r\n"
            + "HOST: [FF02::C]:1900\r\n"
            + "MAN: ssdp:discover\r\n"
            + "MX: " + MAX_WAIT + "\r\n" // server should send response in rand(0, MX)
            + "ST: ssdp:all\r\n"
            + "\r\n";
    
    private UpnpIgdDiscovery() {
        // do nothing
    }
    
    public static Set<UpnpIgdDevice> findIpv4GatewayDevices() throws IOException, InterruptedException {
        InetSocketAddress multicastSocketAddress;
        try {
            multicastSocketAddress = new InetSocketAddress(InetAddress.getByName("239.255.255.250"), 1900);
        } catch (UnknownHostException uhe) {
            throw new IllegalStateException(uhe);
        }
        
        Set<InetAddress> localIpv4Addresses = NetworkUtils.getAllLocalIpv4Addresses();
        return scanAddressesForDevice(multicastSocketAddress, localIpv4Addresses, IPV4_SEARCH_QUERY);
    }

    public static Set<UpnpIgdDevice> findIpv6GatewayDevices() throws IOException, InterruptedException {
        InetSocketAddress multicastSocketAddress;
        try {
            multicastSocketAddress = new InetSocketAddress(InetAddress.getByName("ff02::c"), 1900);
        } catch (UnknownHostException uhe) {
            throw new IllegalStateException(uhe);
        }
        
        Set<InetAddress> localIpv6Addresses = NetworkUtils.getAllLocalIpv6Addresses();
        return scanAddressesForDevice(multicastSocketAddress, localIpv6Addresses, IPV6_SEARCH_QUERY);
    }

    private static Set<UpnpIgdDevice> scanAddressesForDevice(InetSocketAddress multicastSocketAddress, Set<InetAddress> localAddresses,
            String searchQuery) throws IOException, InterruptedException {
        
        
        final Set<UpnpIgdDevice> ret = Collections.synchronizedSet(new HashSet<UpnpIgdDevice>());
        
        UdpCommunicatorListener listener = new UdpCommunicatorListener() {

            @Override
            public void incomingPacket(InetSocketAddress sourceAddress, Channel channel, ByteBuffer packet) {
                byte[] inPacket = ByteBufferUtils.copyContentsToArray(packet);
                
                String inStr;
                try {
                    inStr = new String(inPacket, 0, inPacket.length, "US-ASCII");
                } catch (UnsupportedEncodingException uee) {
                    return;
                }

                Matcher matcher;

                URL url;
                if ((matcher = LOCATION_PATTERN.matcher(inStr)).find()) {
                    String urlStr = matcher.group(1);
                    try {
                        url = new URL(urlStr);
                    } catch (MalformedURLException murle) {
                        return;
                    }
                } else {
                    return;
                }

                String name = null;
                if ((matcher = SERVER_PATTERN.matcher(inStr)).find()) {
                    name = matcher.group(1);
                }

                UpnpIgdDevice device = new UpnpIgdDevice(sourceAddress.getAddress(), name, url);
                ret.add(device);
            }
        };
        
        
        UdpCommunicator comm = null;
        try {
            List<DatagramChannel> channels = new ArrayList<>();

            for (InetAddress localAddr : localAddresses) {
                DatagramChannel channel = DatagramChannel.open();
                channel.configureBlocking(false);
                channel.bind(new InetSocketAddress(localAddr, 0));
                channels.add(channel);
            }
            
            comm = new UdpCommunicator(channels);
            comm.startAsync().awaitRunning();
            comm.addListener(listener);
            
            ByteBuffer searchQueryBuffer = ByteBuffer.wrap(searchQuery.getBytes("US-ASCII")).asReadOnlyBuffer();
            for (int i = 0; i < 3; i++) { 
                for (DatagramChannel channel : channels) {
                    comm.send(channel, multicastSocketAddress, searchQueryBuffer.asReadOnlyBuffer());
                }
                
                Thread.sleep(TimeUnit.SECONDS.toMillis(MAX_WAIT + 1));
            }
            
            return new HashSet<>(ret);
        } finally {
            if (comm != null) {
                try {
                    comm.stopAsync().awaitTerminated(); // this stop should handle closing all the datagram channels
                } catch (IllegalStateException ise) { // NOPMD
                    // do nothing
                }
            }
        }
    }

    public static void main(String[] args) throws Throwable {
        
        System.out.println(findIpv4GatewayDevices());
        System.out.println(findIpv6GatewayDevices());
    }
}
