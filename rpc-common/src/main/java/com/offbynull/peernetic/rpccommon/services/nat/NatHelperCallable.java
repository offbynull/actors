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
package com.offbynull.peernetic.rpccommon.services.nat;

import com.offbynull.peernetic.rpc.Rpc;
import com.offbynull.peernetic.rpc.TcpTransportFactory;
import com.offbynull.peernetic.rpc.TransportFactory;
import com.offbynull.peernetic.rpc.UdpTransportFactory;
import com.offbynull.peernetic.rpc.transport.CompositeIncomingFilter;
import com.offbynull.peernetic.rpc.transport.CompositeOutgoingFilter;
import com.offbynull.peernetic.rpc.transport.IncomingFilter;
import com.offbynull.peernetic.rpc.transport.IncomingMessage;
import com.offbynull.peernetic.rpc.transport.IncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.IncomingMessageResponseHandler;
import com.offbynull.peernetic.rpc.transport.OutgoingFilter;
import com.offbynull.peernetic.rpc.transport.Transport;
import com.offbynull.peernetic.rpccommon.services.nat.NatHelperService.ConnectionType;
import com.offbynull.peernetic.rpccommon.services.nat.NatHelperCallable.Result;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

/**
 * Encapsulates the logic to query a {@link NatHelperService}.
 * @author Kasra F
 */
public final class NatHelperCallable implements Callable<Result> {
    
    private static final IncomingFilter<InetSocketAddress> EMPTY_INCOMING_FILTER =
            new CompositeIncomingFilter<>(Collections.<IncomingFilter<InetSocketAddress>>emptyList());
    private static final OutgoingFilter<InetSocketAddress> EMPTY_OUTGOING_FILTER =
            new CompositeOutgoingFilter<>(Collections.<OutgoingFilter<InetSocketAddress>>emptyList());
    
    private static final int MIN_PORT = 10001;
    private static final int MAX_PORT = 65535;
    
    
    private Rpc<InetSocketAddress> mainRpc;
    private int mainRpcPort;
    private InetSocketAddress partnerAddress;

    /**
     * Constructs a {@link NatHelperCallable} object.
     * @param mainRpc rpc
     * @param mainRpcPort rpc port
     * @param partnerAddress query address
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if {@code mainRpcPort} is not between {@code 1} and {@code 65535}
     */
    public NatHelperCallable(Rpc<InetSocketAddress> mainRpc, int mainRpcPort, InetSocketAddress partnerAddress) {
        Validate.notNull(mainRpc);
        Validate.inclusiveBetween(1, 65535, mainRpcPort);
        Validate.notNull(partnerAddress);
        
        this.mainRpc = mainRpc;
        this.mainRpcPort = mainRpcPort;
        this.partnerAddress = partnerAddress;
    }
    

    @Override
    public Result call() throws Exception {
        Rpc<InetSocketAddress> tcpTestRpc = null;
        Rpc<InetSocketAddress> udpTestRpc = null;

        try {
            NatHelperService service = mainRpc.accessService(partnerAddress, NatHelperService.SERVICE_ID, NatHelperService.class);
            String address = service.getAddress();
            
            InetSocketAddress inetSocketAddress = convertStringAddress(address);
            Set<InetAddress> localAddresses = getLocalAddresses();
            
            boolean addressSameAsLocal = localAddresses.contains(inetSocketAddress.getAddress());
            boolean portSameAsLocal = mainRpcPort == inetSocketAddress.getPort();
            
            
            Random random = new Random();
            
            int tcpPort = MIN_PORT + random.nextInt(MAX_PORT - MIN_PORT);
            TcpTransportFactory tcpTransportFactory = new TcpTransportFactory();
            tcpTransportFactory.setListenAddress(new InetSocketAddress(tcpPort));
            
            int udpPort = MIN_PORT + random.nextInt(MAX_PORT - MIN_PORT);
            UdpTransportFactory udpTransportFactory = new UdpTransportFactory();
            udpTransportFactory.setListenAddress(new InetSocketAddress(udpPort));
            
            byte[] challenge = new byte[8];
            random.nextBytes(challenge);
            
            
            boolean tcpOpen = testServer(service, ConnectionType.TCP, tcpPort, tcpTransportFactory, challenge);
            boolean udpOpen = testServer(service, ConnectionType.UDP, udpPort, udpTransportFactory, challenge);
            
            return new Result(inetSocketAddress, addressSameAsLocal, portSameAsLocal, udpOpen, tcpOpen);
        } finally {
            IOUtils.closeQuietly(tcpTestRpc);
            IOUtils.closeQuietly(udpTestRpc);
        }
    }

    private boolean testServer(NatHelperService service, ConnectionType type, int port,
            TransportFactory<InetSocketAddress> transportFactory, byte[] expected) throws InterruptedException,
            IOException {
        
        Transport<InetSocketAddress> transport = transportFactory.createTransport();

        try {
            final ArrayBlockingQueue<ByteBuffer> bufferHolder = new ArrayBlockingQueue<>(1);
            transport.start(EMPTY_INCOMING_FILTER, new IncomingMessageListener<InetSocketAddress>() {

                @Override
                public void messageArrived(IncomingMessage<InetSocketAddress> message, IncomingMessageResponseHandler responseCallback) {
                    bufferHolder.add(message.getData());
                    responseCallback.terminate();
                }
            }, EMPTY_OUTGOING_FILTER);

            service.testPort(type, port, expected);

            ByteBuffer recievedBuffer = bufferHolder.poll(10000, TimeUnit.MILLISECONDS);

            if (recievedBuffer == null) {
                return false;
            }

            ByteBuffer expectedBuffer = ByteBuffer.wrap(expected);

            return recievedBuffer.equals(expectedBuffer);
        } finally {
            try {
                transport.stop();
            } catch (IOException | RuntimeException e) { // NOPMD
                // do nothing
            }
        }
    }
    
    private InetSocketAddress convertStringAddress(String address) {
        String[] splitAddress = address.split("\\s+");
        if (splitAddress.length != 2) {
            throw new IllegalStateException("Bad address response: " + Arrays.toString(splitAddress));
        }
        
        InetAddress inetAddress;
        try {
             inetAddress = InetAddress.getByName(splitAddress[0]);
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException("Bad address response: " + splitAddress[0], ex);
        }

        // not sure whether to filter these out... the p2p system could be running on a local network
//        if (inetAddress.isLinkLocalAddress() || inetAddress.isSiteLocalAddress() || inetAddress.isLoopbackAddress()) {
//            throw new IllegalArgumentException("Local address response");
//        }
        
        int port;
        try {
            port = Integer.valueOf(splitAddress[1]);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Bad port response: " + splitAddress[1], ex);
        }
        
        Validate.inclusiveBetween(1, 65535, port, "Port response out of range");
        
        return new InetSocketAddress(inetAddress, port);
    }

    private Set<InetAddress> getLocalAddresses() throws SocketException {
        Set<InetAddress> ret = new HashSet<>();
        
        Enumeration e = NetworkInterface.getNetworkInterfaces();
        while (e.hasMoreElements()) {
            NetworkInterface networkInterface = (NetworkInterface) e.nextElement();
            Enumeration niEnum = networkInterface.getInetAddresses();
            while (niEnum.hasMoreElements()) {
                InetAddress address = (InetAddress) niEnum.nextElement();
                
                // not sure whether to filter these out... the p2p system could be running on a local network
//                if (address.isLinkLocalAddress() || address.isSiteLocalAddress() || address.isLoopbackAddress()) {
//                    continue;
//                }
                
                ret.add(address);
            }
        }
        
        return ret;
    }

    /**
     * Result of {@link NatHelperCallable}.
     */
    public static final class Result {

        private InetSocketAddress exposedAddress;
        private boolean exposedAddressMatchesLocalAddress;
        private boolean exposedPortMatchesRpcPort;
        private boolean accessibleUdp;
        private boolean accessibleTcp;

        /**
         * Constructs a {@link Result} object.
         * @param exposedAddress exposed address
         * @param exposedAddressMatchesLocalAddress {@code true} if the exposed address matches a local address, {@code false} otherwise
         * @param exposedPortMatchesRpcPort {@code true} if the exposed port matches the local port, {@code false} otherwise
         * @param accessibleUdp {@code true} if TCP was accessible, {@code false} otherwise
         * @param accessibleTcp {@code true} if UDP was accessible, {@code false} otherwise
         */
        public Result(InetSocketAddress exposedAddress, boolean exposedAddressMatchesLocalAddress, boolean exposedPortMatchesRpcPort,
                boolean accessibleUdp, boolean accessibleTcp) {
            super();
            Validate.notNull(exposedAddress);
            this.exposedAddress = exposedAddress;
            this.exposedAddressMatchesLocalAddress = exposedAddressMatchesLocalAddress;
            this.exposedPortMatchesRpcPort = exposedPortMatchesRpcPort;
            this.accessibleUdp = accessibleUdp;
            this.accessibleTcp = accessibleTcp;
        }

        /**
         * Get the exposed address.
         * @return exposed address
         */
        public InetSocketAddress getExposedAddress() {
            return exposedAddress;
        }

        /**
         * Get if UDP was accessible.
         * @return {@code true} if UDP was accessible, {@code false} otherwise
         */
        public boolean isAccessibleUdp() {
            return accessibleUdp;
        }

        /**
         * Get if TCP was accessible.
         * @return {@code true} if TCP was accessible, {@code false} otherwise
         */
        public boolean isAccessibleTcp() {
            return accessibleTcp;
        }

        /**
         * Get if the exposed address matched a local address.
         * @return {@code true} if the exposed address matched a local address
         */
        public boolean isExposedAddressMatchesLocalAddress() {
            return exposedAddressMatchesLocalAddress;
        }

        /**
         * Get if the exposed port matched the local port.
         * @return {@code true} if the exposed port matched the local port
         */
        public boolean isExposedPortMatchesRpcPort() {
            return exposedPortMatchesRpcPort; // this is totally worthless for tcp
        }

        @Override
        public String toString() {
            return "NatTestResult{" + "exposedAddress=" + exposedAddress + ", exposedAddressMatchesLocalAddress="
                    + exposedAddressMatchesLocalAddress + ", exposedPortMatchesRpcPort=" + exposedPortMatchesRpcPort
                    + ", accessibleUdp=" + accessibleUdp + ", accessibleTcp=" + accessibleTcp + '}';
        }
    }
}
