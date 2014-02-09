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

import com.offbynull.peernetic.router.common.PortType;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;

/**
 * PCP client.
 * @author Kasra Faghihi
 */
public final class PcpClient implements Closeable {
    private PcpCommunicator communicator;
    private InetAddress selfAddress;
    private int sendAttempts;
    private Random random;

    /**
     * Constructs a PCP client.
     * @param random used to generate nonce values for requests
     * @param gatewayAddress address of router/gateway
     * @param selfAddress address of the interface that can talk to the router/gateway
     * @param sendAttempts number of times to try to submit each request
     * @param unsolicitedListener a listener to listen for unsolicited router packets (e.g. for things like ANNOUNCE)
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code sendAttempts < 1 || > 9}
     */
    public PcpClient(Random random, InetAddress gatewayAddress, InetAddress selfAddress, int sendAttempts,
            PcpResponseListener unsolicitedListener) {
        Validate.notNull(random);
        Validate.notNull(gatewayAddress);
        Validate.notNull(selfAddress);
        Validate.inclusiveBetween(1, 9, sendAttempts);
        this.communicator = new PcpCommunicator(gatewayAddress);
        this.selfAddress = selfAddress;
        this.sendAttempts = sendAttempts;
        this.random = random;
        
        this.communicator.startAsync().awaitRunning();
        
        if (unsolicitedListener != null) {
            this.communicator.addListener(unsolicitedListener);
        }
    }
    
    // CHECKSTYLE:OFF custom exception in javadoc not being recognized
    /**
     * Send a ANNOUNCE request to the gateway.
     * @return ANNOUNCE response
     * @throws PcpNoResponseException if no response available
     * @throws InterruptedException if thread was interrupted while waiting
     */
    
    public AnnouncePcpResponse announce() throws InterruptedException {
        // CHECKSTYLE:ON
        AnnouncePcpRequest req = new AnnouncePcpRequest();
        
        AnnounceResponseCreator creator = new AnnounceResponseCreator();
        return performRequest(req, creator);
    }

    // CHECKSTYLE:OFF custom exception in javadoc not being recognized
    /**
     * Send a MAP request to the gateway.
     * @param portType port type
     * @param internalPort internal port ({@code 0} is valid, see Javadoc header)
     * @param suggestedExternalPort suggested external port ({@code 0} for no preference)
     * @param suggestedExternalIpAddress suggested external IP address ({@code ::} for no preference)
     * @param lifetime requested lifetime in seconds
     * @param options PCP options to use
     * @return MAP response
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if any numeric argument is negative, or if {@code protocol > 255}, or if
     * {@code internalPort > 65535}, or if {@code suggestedExternalPort > 65535}
     * remaining, or if {@code protocol == 0} but {@code internalPort != 0}, or if {@code internalPort == 0} but {@code lifetime != 0}
     * @throws PcpNoResponseException if the expected response never came in
     * @throws InterruptedException if thread was interrupted while waiting
     */
    public MapPcpResponse createInboundMapping(PortType portType, int internalPort, int suggestedExternalPort,
            InetAddress suggestedExternalIpAddress, long lifetime, PcpOption ... options) throws InterruptedException {
        // CHECKSTYLE:ON
        byte[] nonce = new byte[12];
        random.nextBytes(nonce);
        
        MapPcpRequest req = new MapPcpRequest(ByteBuffer.wrap(nonce), portType.getProtocolNumber(), internalPort, suggestedExternalPort,
                suggestedExternalIpAddress, lifetime, options);

        MapResponseCreator creator = new MapResponseCreator(req);
        return performRequest(req, creator);
    }
    
    // CHECKSTYLE:OFF custom exception in javadoc not being recognized
    /**
     * Send a PEER request to the gateway.
     * @param portType port type
     * @param internalPort internal port
     * @param suggestedExternalPort suggested external port ({@code 0} for no preference)
     * @param suggestedExternalIpAddress suggested external IP address ({@code null} or {@code ::} for no preference)
     * @param remotePeerPort remote port
     * @param remotePeerIpAddress remote IP address
     * @param lifetime requested lifetime in seconds
     * @param options PCP options to use
     * @return PEER response
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if {@code internalPort < 1 or > 65535}, or if {@code suggestedExternalPort > 65535},
     * or if {@code remotePort < 1 or > 65535}
     * @throws PcpNoResponseException if the expected response never came in
     * @throws InterruptedException if thread was interrupted while waiting
     */
    public PeerPcpResponse createOutboundMapping(PortType portType, int internalPort, int suggestedExternalPort,
            InetAddress suggestedExternalIpAddress, int remotePeerPort, InetAddress remotePeerIpAddress, long lifetime,
            PcpOption ... options) throws InterruptedException {
        // CHECKSTYLE:ON
        byte[] nonce = new byte[12];
        random.nextBytes(nonce);
        
        PeerPcpRequest req = new PeerPcpRequest(ByteBuffer.wrap(nonce), portType.getProtocolNumber(), internalPort, suggestedExternalPort,
                suggestedExternalIpAddress, remotePeerPort, remotePeerIpAddress, lifetime, options);

        PeerResponseCreator creator = new PeerResponseCreator(req);
        return performRequest(req, creator);
    }

    private <T extends PcpResponse> T performRequest(PcpRequest request, Creator<T> creator) throws InterruptedException {
        ByteBuffer sendBuffer = ByteBuffer.allocate(1100);
            
        request.dump(sendBuffer, selfAddress);
        sendBuffer.flip();
        
        for (int i = 1; i <= sendAttempts; i++) {
            T response = attemptRequest(sendBuffer, i, creator);
            if (response != null) {
                return response;
            }
        }
        
        throw new PcpNoResponseException();
    }
    
    private <T extends PcpResponse> T attemptRequest(ByteBuffer sendBuffer, int attempt, Creator<T> creator)
            throws InterruptedException {
        
        final LinkedBlockingQueue<ByteBuffer> recvBufferQueue = new LinkedBlockingQueue<>();
        
        PcpResponseListener listener = new PcpResponseListener() {

            @Override
            public void incomingPacket(CommunicationType type, ByteBuffer packet) {
                if (type != CommunicationType.UNICAST) {
                    return;
                }
                
                recvBufferQueue.add(packet);
            }
        };
        communicator.addListener(listener);
        communicator.send(sendBuffer);

        
        // timeout duration should double each iteration, starting from 250 according to spec
        // i = 1, maxWaitTime = (1 << (1-1)) * 250 = (1 << 0) * 250 = 1 * 250 = 250
        // i = 2, maxWaitTime = (1 << (2-1)) * 250 = (1 << 1) * 250 = 2 * 250 = 500
        // i = 3, maxWaitTime = (1 << (3-1)) * 250 = (1 << 2) * 250 = 4 * 250 = 1000
        // i = 4, maxWaitTime = (1 << (4-1)) * 250 = (1 << 3) * 250 = 8 * 250 = 2000
        // ...
        try {
            int maxWaitTime = (1 << (attempt - 1)) * 250; // NOPMD

            T pcpResponse = null;

            long endTime = System.currentTimeMillis() + maxWaitTime;
            long waitTime;
            while ((waitTime = endTime - System.currentTimeMillis()) > 0L) {
                waitTime = Math.max(waitTime, 0L); // must be at least 0, probably should never happen

                ByteBuffer recvBuffer = recvBufferQueue.poll(waitTime, TimeUnit.MILLISECONDS);

                if (recvBuffer != null) {
                    pcpResponse = creator.create(recvBuffer);
                    if (pcpResponse != null) {
                        break;
                    }
                }
            }

            return pcpResponse;
        } finally {
            communicator.removeListener(listener);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            communicator.stopAsync().awaitTerminated();
        } catch (IllegalStateException iae) {
            throw new IOException(iae);
        }
    }
    
    private interface Creator<T extends PcpResponse> {
        T create(ByteBuffer response);
    }

    private static final class AnnounceResponseCreator implements Creator<AnnouncePcpResponse> {
        public AnnounceResponseCreator() {
        }

        @Override
        public AnnouncePcpResponse create(ByteBuffer recvBuffer) {
            AnnouncePcpResponse response;
            try {
                response = new AnnouncePcpResponse(recvBuffer);
            } catch (BufferUnderflowException | BufferOverflowException | IllegalArgumentException e) {
                //throw new BadResponseException(e);
                return null;
            }

            return response;
        }
    }

    private static final class MapResponseCreator implements Creator<MapPcpResponse> {
        private MapPcpRequest request;

        public MapResponseCreator(MapPcpRequest request) {
            Validate.notNull(request);
            this.request = request;
        }

        @Override
        public MapPcpResponse create(ByteBuffer recvBuffer) {
            MapPcpResponse response;
            try {
                response = new MapPcpResponse(recvBuffer);
            } catch (BufferUnderflowException | BufferOverflowException | IllegalArgumentException e) {
                //throw new BadResponseException(e);
                return null;
            }
            
            if (response.getMappingNonce().equals(request.getMappingNonce())
                    && response.getProtocol() == request.getProtocol()
                    && response.getInternalPort() == request.getInternalPort()) {
                return response;
            }
            
            return null;
        }
    }
    
    private static final class PeerResponseCreator implements Creator<PeerPcpResponse> {
        private PeerPcpRequest request;
        
        public PeerResponseCreator(PeerPcpRequest request) {
            Validate.notNull(request);
            this.request = request;
        }

        @Override
        public PeerPcpResponse create(ByteBuffer recvBuffer) {
            PeerPcpResponse response;
            try {
                response = new PeerPcpResponse(recvBuffer);
            } catch (BufferUnderflowException | BufferOverflowException | IllegalArgumentException e) {
                //throw new BadResponseException(e);
                return null;
            }

            if (response.getMappingNonce().equals(request.getMappingNonce())
                    && response.getProtocol() == request.getProtocol()
                    && response.getRemotePeerPort() == request.getRemotePeerPort()
                    && response.getRemotePeerIpAddress().equals(request.getRemotePeerIpAddress())) {
                return response;
            }
            
            return null;
        }
    }
}
