package com.offbynull.peernetic.router.upnpigd;

import com.offbynull.peernetic.common.utils.ByteBufferUtils;
import com.offbynull.peernetic.router.common.NetworkUtils;
import com.offbynull.peernetic.router.common.UdpCommunicator;
import com.offbynull.peernetic.router.common.UdpCommunicatorListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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

    public static Set<UpnpIgdDevice> findIpv4Devices() throws IOException, InterruptedException {
        InetSocketAddress multicastSocketAddress;
        try {
            multicastSocketAddress = new InetSocketAddress(InetAddress.getByName("239.255.255.250"), 1900);
        } catch (UnknownHostException uhe) {
            throw new IllegalStateException(uhe);
        }

        Set<InetAddress> localIpv4Addresses = NetworkUtils.getAllLocalIpv4Addresses();
        return scanForDevices(multicastSocketAddress, localIpv4Addresses, IPV4_SEARCH_QUERY);
    }

    public static Set<UpnpIgdDevice> findIpv6Devices() throws IOException, InterruptedException {
        InetSocketAddress multicastSocketAddress;
        try {
            multicastSocketAddress = new InetSocketAddress(InetAddress.getByName("ff02::c"), 1900);
        } catch (UnknownHostException uhe) {
            throw new IllegalStateException(uhe);
        }

        Set<InetAddress> localIpv6Addresses = NetworkUtils.getAllLocalIpv6Addresses();
        return scanForDevices(multicastSocketAddress, localIpv6Addresses, IPV6_SEARCH_QUERY);
    }
    
    public static Set<UpnpIgdService> findServices(Set<UpnpIgdDevice> devices) throws InterruptedException {
        Validate.noNullElements(devices);
        
        return scanForServicesInDevices(devices);
    }

    private static Set<UpnpIgdDevice> scanForDevices(InetSocketAddress multicastSocketAddress, Set<InetAddress> localAddresses,
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

                URI url;
                if ((matcher = LOCATION_PATTERN.matcher(inStr)).find()) {
                    String urlStr = matcher.group(1);
                    try {
                        url = new URI(urlStr);
                    } catch (URISyntaxException urise) {
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
    
    private static Set<UpnpIgdService> scanForServicesInDevices(Set<UpnpIgdDevice> devices) throws InterruptedException {
        
        Set<UpnpIgdService> services = Collections.synchronizedSet(new HashSet<UpnpIgdService>());
        CountDownLatch latch = new CountDownLatch(devices.size());
        
        
        CloseableHttpAsyncClient httpclient = null;
        try {
            httpclient = HttpAsyncClients.createDefault();
            httpclient.start();

            for (UpnpIgdDevice device : devices) {
                HttpGet request = new HttpGet(device.getUrl());
                httpclient.execute(request, new ServiceFutureCallback(device, services, latch));
            }

            latch.await();
        } finally {
            IOUtils.closeQuietly(httpclient);
        }
        
        return new HashSet<>(services);
    }

    public static final class ServiceFutureCallback implements FutureCallback<HttpResponse> {
        private final Set<UpnpIgdService> services;
        private final CountDownLatch latch;
        private final UpnpIgdDevice device;

        public ServiceFutureCallback(UpnpIgdDevice device, Set<UpnpIgdService> services, CountDownLatch latch) {
            this.device = device;
            this.services = services;
            this.latch = latch;
        }
        
        @Override
        public void completed(HttpResponse response) {
            InputStream is = null;
            try {
                is = response.getEntity().getContent();

                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(is);

                XPath xPath = XPathFactory.newInstance().newXPath();
                NodeList serviceNodes = (NodeList) xPath.compile(".//service").evaluate(doc, XPathConstants.NODESET);

                for (int i = 0; i < serviceNodes.getLength(); i++) {
                    Node serviceNode = serviceNodes.item(i);

                    String serviceType = StringUtils.trim(xPath.compile("serviceType").evaluate(serviceNode));
                    String serviceId = StringUtils.trim(xPath.compile("serviceId").evaluate(serviceNode));
                    String controlUrl = StringUtils.trim(xPath.compile("controlURL").evaluate(serviceNode));
                    String eventSubUrl = StringUtils.trim(xPath.compile("eventSubURL").evaluate(serviceNode));
                    String scpdUrl = StringUtils.trim(xPath.compile("SCPDURL").evaluate(serviceNode));

                    UpnpIgdService service = new UpnpIgdService(device, serviceType, serviceId, controlUrl, eventSubUrl, scpdUrl);
                    services.add(service);
                }
            } catch (SAXException | ParserConfigurationException | IOException | XPathExpressionException e) { // NOPMD
                // do nothing
            } finally {
                IOUtils.closeQuietly(is);
                latch.countDown();
            }
        }

        @Override
        public void failed(Exception ex) {
            latch.countDown();
        }

        @Override
        public void cancelled() {
            latch.countDown();
        }
    }
    
    public static void main(String[] args) throws Throwable {

        Set<UpnpIgdDevice> devices = new HashSet<>();
        devices.addAll(findIpv4Devices());
        devices.addAll(findIpv6Devices());
        
        Set<UpnpIgdService> services = findServices(devices);
        for (UpnpIgdService service : services) {
            System.out.println(service);
        }
    }
}
