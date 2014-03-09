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
package com.offbynull.peernetic.router.natpmp;

import com.offbynull.peernetic.router.common.CommunicationType;
import com.offbynull.peernetic.router.common.ResponseException;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;

/**
 * Accesses NAT-PMP features of a gateway/router.
 * @author Kasra Faghihi
 */
public final class NatPmpController implements Closeable {

    private NatPmpCommunicator communicator;

    /**
     * Constructs a {@link NatPmpController} object.
     * @param gatewayAddress address of router/gateway
     * @param listener a listener to listen for all NAT-PMP packets from this router
     * @throws NullPointerException if any argument is {@code null}
     */
    public NatPmpController(InetAddress gatewayAddress, final NatPmpControllerListener listener) {
        Validate.notNull(gatewayAddress);
        
        this.communicator = new NatPmpCommunicator(gatewayAddress);
        this.communicator.startAsync().awaitRunning();
        
        if (listener != null) {
            this.communicator.addListener(new NatPmpCommunicatorListener() {

                @Override
                public void incomingPacket(CommunicationType type, ByteBuffer packet) {
                    try {
                        packet.mark();
                        listener.incomingResponse(type, new ExternalAddressNatPmpResponse(packet));
                    } catch (BufferUnderflowException | IllegalArgumentException e) { // NOPMD
                        // ignore
                    } finally {
                        packet.reset();
                    }
                    
                    try {
                        packet.mark();
                        listener.incomingResponse(type, new UdpMappingNatPmpResponse(packet));
                    } catch (BufferUnderflowException | IllegalArgumentException e) { // NOPMD
                        // ignore
                    } finally {
                        packet.reset();
                    }
                    
                    try {
                        packet.mark();
                        listener.incomingResponse(type, new TcpMappingNatPmpResponse(packet));
                    } catch (BufferUnderflowException | IllegalArgumentException e) { // NOPMD
                        // ignore
                    } finally {
                        packet.reset();
                    }
                }
            });
        }
    }
    
    // CHECKSTYLE:OFF custom exception in javadoc not being recognized
    /**
     * Send an external address request to the gateway.
     * @param sendAttempts number of times to try to submit each request
     * @return external address response
     * @throws BufferUnderflowException if the message is too big to be written in to the buffer
     * @throws NoResponseExceptionf no response available
     * @throws InterruptedException if thread was interrupted while waiting
     * @throws IllegalArgumentException if {@code sendAttempts < 1 || > 9}
     */
    public ExternalAddressNatPmpResponse requestExternalAddress(int sendAttempts) throws InterruptedException {
        // CHECKSTYLE:ON
        ExternalAddressNatPmpRequest req = new ExternalAddressNatPmpRequest();
        
        ExternalAddressNatPmpResponseCreator creator = new ExternalAddressNatPmpResponseCreator();
        return performRequest(sendAttempts, req, creator);
    }

    // CHECKSTYLE:OFF custom exception in javadoc not being recognized
    /**
     * Send a UDP map request to the gateway.
     * @param sendAttempts number of times to try to submit each request
     * @param internalPort internal port
     * @param suggestedExternalPort suggested external port ({@code 0} for no preference)
     * @param lifetime requested lifetime in seconds
     * @return MAP response
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws BufferUnderflowException if the message is too big to be written in to the buffer
     * @throws IllegalArgumentException if any numeric argument is negative, or if {@code internalPort < 1 || > 65535}, or if
     * {@code suggestedExternalPort > 65535}, or if {@code sendAttempts < 1 || > 9}
     * @throws NoReResponseExceptionthe expected response never came in
     * @throws InterruptedException if thread was interrupted while waiting
     */
    public UdpMappingNatPmpResponse requestUdpMappingOperation(int sendAttempts, int internalPort, int suggestedExternalPort,
            long lifetime) throws InterruptedException {
        // CHECKSTYLE:ON
        UdpMappingNatPmpRequest req = new UdpMappingNatPmpRequest(internalPort, suggestedExternalPort, lifetime);

        RequestUdpMappingNatPmpResponseCreator creator = new RequestUdpMappingNatPmpResponseCreator(req);
        return performRequest(sendAttempts, req, creator);
    }

    // CHECKSTYLE:OFF custom exception in javadoc not being recognized
    /**
     * Send a TCP map request to the gateway.
     * @param sendAttempts number of times to try to submit each request
     * @param internalPort internal port
     * @param suggestedExternalPort suggested external port ({@code 0} for no preference)
     * @param lifetime requested lifetime in seconds
     * @return MAP response
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws BufferUnderflowException if the message is too big to be written in to the buffer
     * @throws IllegalArgumentException if any numeric argument is negative, or if {@code internalPort < 1 || > 65535}, or if
     * {@code suggestedExternalPort > 65535}, or if {@code sendAttempts < 1 || > 9}
     * @throws NoRespResponseExceptione expected response never came in
     * @throws InterruptedException if thread was interrupted while waiting
     */
    public TcpMappingNatPmpResponse requestTcpMappingOperation(int sendAttempts, int internalPort, int suggestedExternalPort,
            long lifetime) throws InterruptedException {
        // CHECKSTYLE:ON
        TcpMappingNatPmpRequest req = new TcpMappingNatPmpRequest(internalPort, suggestedExternalPort, lifetime);

        RequestTcpMappingNatPmpResponseCreator creator = new RequestTcpMappingNatPmpResponseCreator(req);
        return performRequest(sendAttempts, req, creator);
    }

    private <T extends NatPmpResponse> T performRequest(int sendAttempts, NatPmpRequest request, Creator<T> creator)
            throws InterruptedException {
        Validate.inclusiveBetween(1, 9, sendAttempts);
        
        ByteBuffer sendBuffer = ByteBuffer.allocate(12);
            
        request.dump(sendBuffer);
        sendBuffer.flip();
        
        for (int i = 1; i <= sendAttempts; i++) {
            T response = attemptRequest(sendBuffer, i, creator);
            if (response != null) {
                return response;
            }
        }
        
        throw new ResponseException();
    }


    private <T extends NatPmpResponse> T attemptRequest(ByteBuffer sendBuffer, int attempt, Creator<T> creator)
            throws InterruptedException {
        
        final LinkedBlockingQueue<ByteBuffer> recvBufferQueue = new LinkedBlockingQueue<>();
        
        NatPmpCommunicatorListener listener = new NatPmpCommunicatorListener() {

            @Override
            public void incomingPacket(CommunicationType type, ByteBuffer packet) {
                if (type != CommunicationType.UNICAST) {
                    return;
                }
                
                recvBufferQueue.add(packet);
            }
        };

        
        // timeout duration should double each iteration, starting from 250 according to spec
        // i = 1, maxWaitTime = (1 << (1-1)) * 250 = (1 << 0) * 250 = 1 * 250 = 250
        // i = 2, maxWaitTime = (1 << (2-1)) * 250 = (1 << 1) * 250 = 2 * 250 = 500
        // i = 3, maxWaitTime = (1 << (3-1)) * 250 = (1 << 2) * 250 = 4 * 250 = 1000
        // i = 4, maxWaitTime = (1 << (4-1)) * 250 = (1 << 3) * 250 = 8 * 250 = 2000
        // ...
        try {
            communicator.addListener(listener);
            communicator.send(sendBuffer);

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
    
    private interface Creator<T extends NatPmpResponse> {
        T create(ByteBuffer response);
    }

    private static final class ExternalAddressNatPmpResponseCreator implements Creator<ExternalAddressNatPmpResponse> {
        public ExternalAddressNatPmpResponseCreator() {
        }

        @Override
        public ExternalAddressNatPmpResponse create(ByteBuffer recvBuffer) {
            ExternalAddressNatPmpResponse response;
            try {
                response = new ExternalAddressNatPmpResponse(recvBuffer);
            } catch (BufferUnderflowException | BufferOverflowException | IllegalArgumentException e) {
                //throw new BadResponseException(e);
                return null;
            }

            return response;
        }
    }

    private static final class RequestUdpMappingNatPmpResponseCreator implements Creator<UdpMappingNatPmpResponse> {
        private UdpMappingNatPmpRequest request;

        public RequestUdpMappingNatPmpResponseCreator(UdpMappingNatPmpRequest request) {
            Validate.notNull(request);
            this.request = request;
        }

        @Override
        public UdpMappingNatPmpResponse create(ByteBuffer recvBuffer) {
            UdpMappingNatPmpResponse response;
            try {
                response = new UdpMappingNatPmpResponse(recvBuffer);
            } catch (BufferUnderflowException | BufferOverflowException | IllegalArgumentException e) {
                //throw new BadResponseException(e);
                return null;
            }
            
            if (response.getInternalPort() == request.getInternalPort()) {
                return response;
            }
            
            return null;
        }
    }
    
    private static final class RequestTcpMappingNatPmpResponseCreator implements Creator<TcpMappingNatPmpResponse> {
        private TcpMappingNatPmpRequest request;

        public RequestTcpMappingNatPmpResponseCreator(TcpMappingNatPmpRequest request) {
            Validate.notNull(request);
            this.request = request;
        }

        @Override
        public TcpMappingNatPmpResponse create(ByteBuffer recvBuffer) {
            TcpMappingNatPmpResponse response;
            try {
                response = new TcpMappingNatPmpResponse(recvBuffer);
            } catch (BufferUnderflowException | BufferOverflowException | IllegalArgumentException e) {
                //throw new BadResponseException(e);
                return null;
            }
            
            if (response.getInternalPort() == request.getInternalPort()) {
                return response;
            }
            
            return null;
        }
    }
}
