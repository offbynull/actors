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
import com.offbynull.peernetic.router.common.NoResponseException;
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
                        listener.incomingResponse(type, new RequestUdpMappingNatPmpResponse(packet));
                    } catch (BufferUnderflowException | IllegalArgumentException e) { // NOPMD
                        // ignore
                    } finally {
                        packet.reset();
                    }
                    
                    try {
                        packet.mark();
                        listener.incomingResponse(type, new RequestTcpMappingNatPmpResponse(packet));
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
     * @throws NoResponseException if no response available
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
     * @throws NoResponseException if the expected response never came in
     * @throws InterruptedException if thread was interrupted while waiting
     */
    public RequestUdpMappingNatPmpResponse requestUdpMappingOperation(int sendAttempts, int internalPort, int suggestedExternalPort,
            long lifetime) throws InterruptedException {
        // CHECKSTYLE:ON
        RequestUdpMappingNatPmpRequest req = new RequestUdpMappingNatPmpRequest(internalPort, suggestedExternalPort, lifetime);

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
     * @throws NoResponseException if the expected response never came in
     * @throws InterruptedException if thread was interrupted while waiting
     */
    public RequestTcpMappingNatPmpResponse requestTcpMappingOperation(int sendAttempts, int internalPort, int suggestedExternalPort,
            long lifetime) throws InterruptedException {
        // CHECKSTYLE:ON
        RequestTcpMappingNatPmpRequest req = new RequestTcpMappingNatPmpRequest(internalPort, suggestedExternalPort, lifetime);

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
        
        throw new NoResponseException();
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

    private static final class RequestUdpMappingNatPmpResponseCreator implements Creator<RequestUdpMappingNatPmpResponse> {
        private RequestUdpMappingNatPmpRequest request;

        public RequestUdpMappingNatPmpResponseCreator(RequestUdpMappingNatPmpRequest request) {
            Validate.notNull(request);
            this.request = request;
        }

        @Override
        public RequestUdpMappingNatPmpResponse create(ByteBuffer recvBuffer) {
            RequestUdpMappingNatPmpResponse response;
            try {
                response = new RequestUdpMappingNatPmpResponse(recvBuffer);
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
    
    private static final class RequestTcpMappingNatPmpResponseCreator implements Creator<RequestTcpMappingNatPmpResponse> {
        private RequestTcpMappingNatPmpRequest request;

        public RequestTcpMappingNatPmpResponseCreator(RequestTcpMappingNatPmpRequest request) {
            Validate.notNull(request);
            this.request = request;
        }

        @Override
        public RequestTcpMappingNatPmpResponse create(ByteBuffer recvBuffer) {
            RequestTcpMappingNatPmpResponse response;
            try {
                response = new RequestTcpMappingNatPmpResponse(recvBuffer);
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
//    /**
//     * Query the router/gateway for its external IP address. {@link NatPmpReceiver} allows you to receive updates on external address
//     * changes, but doesn't seem to be supported by some routers. Periodically query this method for best results.
//     *
//     * @return external IP address
//     * @throws IOException if there's a problem with the socket or there is no response / there's an unexpected response
//     */
//    public ExternalAddressNatPmpResponse getExternalAddress() throws IOException {
//        DatagramSocket datagramSocket = null;
//        ByteBuffer requestBuffer = ByteBuffer.allocate(12);
//        ByteBuffer responseBuffer = ByteBuffer.allocate(16);
//
//        try {
//            datagramSocket = new DatagramSocket(0);
//
//            //    0                   1
//            //    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Vers = 0      | OP = 0        |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            requestBuffer.put((byte) 0);
//            requestBuffer.put((byte) 0);
//            requestBuffer.flip();
//
//            performRequest(datagramSocket, requestBuffer, responseBuffer, 12, 128);
//
//            //    0                   1                   2                   3
//            //    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Vers = 0      | OP = 128 + 0  | Result Code                   |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Seconds Since Start of Epoch                                  |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | External IP Address (a.b.c.d)                                 |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            return new ExternalAddressNatPmpResponse(responseBuffer);
//        } finally {
//            IOUtils.closeQuietly(datagramSocket);
//        }
//    }
//
//    /**
//     * Request that the gateway/router forward a port to this machine. Periodically call
//     * {@link #createMapping(com.offbynull.peernetic.router.natpmp.PortType, int, int) } with the same {@code portType} and
//     * {@code internalPort} to keep the port open, otherwise the port will automatically close. The recommended amount of time to wait
//     * before calls should be half of the {@code lifetime} property from the return of this method
//     * ({@link CreateMappingResult#getLifetime() }).
//     *
//     * @param portType port type
//     * @param internalPort local port to be forwarded to
//     * @param externalPort external port to be forwarded from (router doesn't have to respect this -- true port in result)
//     * @param lifetime number of seconds that {@code externalPort} should remain open for (router doesn't have to respect this -- true
//     * lifetime in result), recommended to set to {@code 3600}
//     * @return router/gateway's response
//     * @throws IOException if there's a problem with the socket or there is no response / there's an unexpected response
//     * @throws NullPointerException if any argument is {@code null}
//     * @throws IllegalArgumentException if any numeric argument is not positive ({@code <= 0})
//     */
//    public RequestUdpMappingNatPmpResponse createMapping(PortType portType, int internalPort, int externalPort, int lifetime) throws IOException {
//        Validate.notNull(portType);
//        Validate.inclusiveBetween(1, 65535, internalPort);
//        Validate.inclusiveBetween(1, 65535, externalPort); // 0 = high-numbered "anonymous" port
//        Validate.inclusiveBetween(1, Integer.MAX_VALUE, lifetime);
//
//        DatagramSocket datagramSocket = null;
//        ByteBuffer requestBuffer = ByteBuffer.allocate(12);
//        ByteBuffer responseBuffer = ByteBuffer.allocate(16);
//
//        try {
//            datagramSocket = new DatagramSocket(0);
//            
//            //    0                   1                   2                   3
//            //    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Vers = 0      | OP = x        | Reserved (MUST be zero)       |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Internal Port                 | Requested External Port       |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Requested Port Mapping Lifetime in Seconds                    |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //
//            //   Opcodes supported:
//            //   1 - Map UDP
//            //   2 - Map TCP
//            byte op;
//            switch (portType) {
//                case UDP:
//                    op = 1;
//                    break;
//                case TCP:
//                    op = 2;
//                    break;
//                default:
//                    throw new IllegalArgumentException();
//            }
//
//            requestBuffer.put((byte) 0);
//            requestBuffer.put(op);
//            requestBuffer.putShort((short) 0);
//            requestBuffer.putShort((short) internalPort);
//            requestBuffer.putShort((short) externalPort);
//            requestBuffer.putInt(lifetime); // The RECOMMENDED Port Mapping Lifetime is 3600 seconds.
//            requestBuffer.flip();
//
//            //    0                   1                   2                   3
//            //    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Vers = 0      | OP = 128 + x  | Result Code                   |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Seconds Since Start of Epoch                                  |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Internal Port                 | Mapped External Port          |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Port Mapping Lifetime in Seconds                              |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            performRequest(datagramSocket, requestBuffer, responseBuffer, 16, 128 + op);
//
//            try {
//                return new RequestUdpMappingNatPmpResponse(responseBuffer, internalPort);
//            } catch (IllegalArgumentException iae) {
//                throw new IOException(iae);
//            }
//        } finally {
//            IOUtils.closeQuietly(datagramSocket);
//        }
//    }
//
//    /**
//     * Request that the gateway/router forward a port to this machine using "a high-numbered 'anonymous' external port". Periodically call
//     * this method with the same {@code portType} and {@code internalPort} to keep the port open, otherwise the port will automatically
//     * close. The recommended amount of time to wait before calls should be half of the {@code lifetime} property from the return of this
//     * method ({@link CreateMappingResult#getLifetime() }).
//     *
//     * @param portType port type
//     * @param internalPort local port to be forwarded to -- if this port is already open, it'll be refreshed
//     * @param lifetime number of seconds that {@code externalPort} should remain open for (router doesn't have to respect this -- true
//     * lifetime in result), recommended to set to {@code 3600}
//     * @return router/gateway's response
//     * @throws IOException if there's a problem with the socket or there is no response / there's an unexpected response
//     * @throws NullPointerException if any argument is {@code null}
//     * @throws IllegalArgumentException if any numeric argument is not positive ({@code <= 0})
//     */
//    public RequestUdpMappingNatPmpResponse createMapping(PortType portType, int internalPort, int lifetime) throws IOException {
//        Validate.notNull(portType);
//        Validate.inclusiveBetween(1, 65535, internalPort);
//        Validate.inclusiveBetween(1, Integer.MAX_VALUE, lifetime);
//
//        DatagramSocket datagramSocket = null;
//        ByteBuffer requestBuffer = ByteBuffer.allocate(12);
//        ByteBuffer responseBuffer = ByteBuffer.allocate(16);
//
//        try {
//            datagramSocket = new DatagramSocket(0);
//            
//            //   If the client would prefer to have a high-numbered "anonymous"
//            //   external port assigned, then it should set the Requested External
//            //   Port to zero, which indicates to the gateway that it should allocate
//            //   a high-numbered port of its choosing. If the client would prefer
//            //   instead to have the mapped external port be the same as its local
//            //   Internal Port if possible (e.g. a web server listening on port 80
//            //   that would ideally like to have external port 80) then it should set
//            //   the Requested External Port to the desired value. However, the
//            //   gateway is not obliged to assign the port requested, and may choose
//            //   not to, either for policy reasons (e.g. port 80 is reserved and
//            //   clients may not request it) or because that port has already been
//            //   assigned to some other client. Because of this, some product
//            //   developers have questioned the value of having the Requested External
//            //   Port field at all. The reason is for failure recovery. Most low-cost
//            //   home NAT gateways do not record temporary port mappings in persistent
//            //   storage, so if the gateway crashes or is rebooted, all the mappings
//            //   are lost. A renewal packet is formatted identically to an initial
//            //   mapping request packet, except that for renewals the client sets the
//            //   Requested External Port field to the port the gateway actually
//            //   assigned, rather than the port the client originally wanted. When a
//            //   freshly-rebooted NAT gateway receives a renewal packet from a client,
//            //   it appears to the gateway just like an ordinary initial request for
//            //   a port mapping, except that in this case the Requested External Port
//            //   is likely to be one that the NAT gateway *is* willing to allocate
//            //   (it allocated it to this client right before the reboot, so it should
//            //   presumably be willing to allocate it again).
//            //    0                   1                   2                   3
//            //    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Vers = 0      | OP = x        | Reserved (MUST be zero)       |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Internal Port                 | Requested External Port       |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Requested Port Mapping Lifetime in Seconds                    |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //
//            //   Opcodes supported:
//            //   1 - Map UDP
//            //   2 - Map TCP
//            byte op;
//            switch (portType) {
//                case UDP:
//                    op = 1;
//                    break;
//                case TCP:
//                    op = 2;
//                    break;
//                default:
//                    throw new IllegalArgumentException();
//            }
//
//            requestBuffer.put((byte) 0);
//            requestBuffer.put(op);
//            requestBuffer.putShort((short) 0);
//            requestBuffer.putShort((short) internalPort);
//            requestBuffer.putShort((short) 0);
//            requestBuffer.putInt(lifetime); // The RECOMMENDED Port Mapping Lifetime is 3600 seconds.
//            requestBuffer.flip();
//
//            //    0                   1                   2                   3
//            //    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Vers = 0      | OP = 128 + x  | Result Code                   |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Seconds Since Start of Epoch                                  |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Internal Port                 | Mapped External Port          |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Port Mapping Lifetime in Seconds                              |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            performRequest(datagramSocket, requestBuffer, responseBuffer, 16, 128 + op);
//
//            try {
//                return new RequestUdpMappingNatPmpResponse(responseBuffer, internalPort);
//            } catch (IllegalArgumentException iae) {
//                throw new IOException(iae);
//            }
//        } finally {
//            IOUtils.closeQuietly(datagramSocket);
//        }
//    }
//
//    /**
//     * Request that the gateway/router stop forwarding a port to this machine.
//     *
//     * @param portType port type
//     * @param internalPort local port to stop forwarding to
//     * @throws IOException if there's a problem with the socket or there is no response / there's an unexpected response
//     * @throws NullPointerException if any argument is {@code null}
//     * @throws IllegalArgumentException if any numeric argument is not positive ({@code <= 0})
//     */
//    public void deleteMapping(PortType portType, int internalPort) throws IOException {
//        Validate.notNull(portType);
//        Validate.inclusiveBetween(1, 65535, internalPort);
//
//        DatagramSocket datagramSocket = null;
//        ByteBuffer requestBuffer = ByteBuffer.allocate(12);
//        ByteBuffer responseBuffer = ByteBuffer.allocate(16);
//
//        try {
//            datagramSocket = new DatagramSocket(0);
//            
//            //   A client MAY also send an explicit packet to request deletion of a
//            //   mapping that is no longer needed. A client requests explicit
//            //   deletion of a mapping by sending a message to the NAT gateway
//            //   requesting the mapping, with the Requested Lifetime in Seconds set
//            //   to 0. The requested external port MUST be set to zero by the client
//            //   on sending, and MUST be ignored by the gateway on reception.
//            //
//            //   When a mapping is destroyed successfully as a result of the client
//            //   explicitly requesting the deletion, the NAT gateway MUST send a
//            //   response packet which is formatted as defined in Section 3.3
//            //   "Creating a Mapping". The response MUST contain a result code of 0,
//            //   the internal port as indicated in the deletion request, an external
//            //   port of 0, and a lifetime of 0. The NAT gateway MUST respond to
//            //   a request to destroy a mapping that does not exist as if the
//            //   request were successful. This is because of the case where the
//            //   acknowledgement is lost, and the client retransmits its request to
//            //   delete the mapping. In this case the second request to delete the
//            //   mapping MUST return the same response packet as the first request.
//            //
//            //   If the deletion request was unsuccessful, the response MUST contain
//            //   a non-zero result code and the requested mapping; the lifetime is
//            //   undefined (MUST be set to zero on transmission, and MUST be ignored
//            //   on reception). If the client attempts to delete a port mapping which
//            //   was manually assigned by some kind of configuration tool, the NAT
//            //   gateway MUST respond with a 'Not Authorized' error, result code 2.
//            //    0                   1                   2                   3
//            //    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Vers = 0      | OP = x        | Reserved (MUST be zero)       |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Internal Port                 | Requested External Port       |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Requested Port Mapping Lifetime in Seconds                    |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //
//            //   Opcodes supported:
//            //   1 - Map UDP
//            //   2 - Map TCP
//            byte op;
//            switch (portType) {
//                case UDP:
//                    op = 1;
//                    break;
//                case TCP:
//                    op = 2;
//                    break;
//                default:
//                    throw new IllegalArgumentException();
//            }
//
//            requestBuffer.put((byte) 0);
//            requestBuffer.put(op);
//            requestBuffer.putShort((short) 0);
//            requestBuffer.putShort((short) internalPort);
//            requestBuffer.putShort((short) 0);
//            requestBuffer.putInt(0);
//            requestBuffer.flip();
//
//            //    0                   1                   2                   3
//            //    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Vers = 0      | OP = 128 + x  | Result Code                   |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Seconds Since Start of Epoch                                  |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Internal Port                 | Mapped External Port          |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Port Mapping Lifetime in Seconds                              |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            performRequest(datagramSocket, requestBuffer, responseBuffer, 16, 128 + op);
//        } finally {
//            IOUtils.closeQuietly(datagramSocket);
//        }
//    }
//
//    /**
//     * Request that the gateway/router stop forwarding all ports of a certain type (TCP/UDP) port to this machine. Be careful when using
//     * this method as it will destroy forwards made by all applications on this machine.
//     *
//     * @param portType port type
//     * @throws IOException if there's a problem with the socket or there is no response / there's an unexpected response
//     * @throws NullPointerException if any argument is {@code null}
//     */
//    public void deleteMappings(PortType portType) throws IOException {
//        Validate.notNull(portType);
//
//        DatagramSocket datagramSocket = null;
//        ByteBuffer requestBuffer = ByteBuffer.allocate(12);
//        ByteBuffer responseBuffer = ByteBuffer.allocate(16);
//
//        try {
//            datagramSocket = new DatagramSocket(0);
//            
//            //   A client can request the explicit deletion of all its UDP or TCP
//            //   mappings by sending the same deletion request to the NAT gateway with
//            //   external port, internal port, and lifetime set to 0. A client MAY
//            //   choose to do this when it first acquires a new IP address in order to
//            //   protect itself from port mappings that were performed by a previous
//            //   owner of the IP address. After receiving such a deletion request, the
//            //   gateway MUST delete all its UDP or TCP port mappings (depending on
//            //   the opcode). The gateway responds to such a deletion request with a
//            //   response as described above, with the internal port set to zero. If
//            //   the gateway is unable to delete a port mapping, for example, because
//            //   the mapping was manually configured by the administrator, the gateway
//            //   MUST still delete as many port mappings as possible, but respond with
//            //   a non-zero result code. The exact result code to return depends on
//            //   the cause of the failure. If the gateway is able to successfully
//            //   delete all port mappings as requested, it MUST respond with a result
//            //   code of 0.
//            //    0                   1                   2                   3
//            //    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Vers = 0      | OP = x        | Reserved (MUST be zero)       |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Internal Port                 | Requested External Port       |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Requested Port Mapping Lifetime in Seconds                    |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //
//            //   Opcodes supported:
//            //   1 - Map UDP
//            //   2 - Map TCP
//            byte op;
//            switch (portType) {
//                case UDP:
//                    op = 1;
//                    break;
//                case TCP:
//                    op = 2;
//                    break;
//                default:
//                    throw new IllegalArgumentException();
//            }
//
//            requestBuffer.put((byte) 0);
//            requestBuffer.put(op);
//            requestBuffer.putShort((short) 0);
//            requestBuffer.putShort((short) 0);
//            requestBuffer.putShort((short) 0);
//            requestBuffer.putInt(0); // The RECOMMENDED Port Mapping Lifetime is 3600 seconds.
//            requestBuffer.flip();
//
//            //    0                   1                   2                   3
//            //    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Vers = 0      | OP = 128 + x  | Result Code                   |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Seconds Since Start of Epoch                                  |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Internal Port                 | Mapped External Port          |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            //   | Port Mapping Lifetime in Seconds                              |
//            //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//            performRequest(datagramSocket, requestBuffer, responseBuffer, 16, 128 + op);
//        } finally {
//            IOUtils.closeQuietly(datagramSocket);
//        }
//    }
//
//    private void performRequest(DatagramSocket socket, ByteBuffer requestBuffer, ByteBuffer responseBuffer,
//            int successfulResponseSize, int successfulResponseOpcode) throws IOException {
//        
//        byte[] requestCopy = ByteBufferUtils.copyContentsToArray(requestBuffer);
//
//        for (int i = 1; i <= sendAttempts; i++) {
//            // timeout duration should double each iteration, starting from 250 according to spec
//            // i = 1, timeoutDuration = (1 << (1-1)) * 250 = (1 << 0) * 250 = 1 * 250 = 250
//            // i = 2, timeoutDuration = (1 << (2-1)) * 250 = (1 << 1) * 250 = 2 * 250 = 500
//            // i = 3, timeoutDuration = (1 << (3-1)) * 250 = (1 << 2) * 250 = 4 * 250 = 1000
//            // i = 4, timeoutDuration = (1 << (4-1)) * 250 = (1 << 3) * 250 = 8 * 250 = 2000
//            // ...
//            int timeoutDuration = (1 << (i - 1)) * 250; // NOPMD
//            socket.setSoTimeout(timeoutDuration);
//
//            DatagramPacket request = new DatagramPacket(requestCopy, requestCopy.length, gatewayAddress, 5351);
//            socket.send(request);
//
//            DatagramPacket response = new DatagramPacket(responseBuffer.array(), responseBuffer.capacity());
//            try {
//                socket.receive(response);
//            } catch (SocketTimeoutException ste) {
//                continue;
//            }
//
//            if (!response.getAddress().equals(gatewayAddress)) { // data isn't from our gateway, ignore
//                continue;
//            }
//
//            responseBuffer.position(response.getLength());
//            responseBuffer.flip();
//
//            if (responseBuffer.remaining() < 4) { // must contain atleast version(1), opcode(1), resultcode(2)
//                throw new IOException("Not enough bytes in response: " + responseBuffer.remaining());
//            }
//
//            if (responseBuffer.remaining() != successfulResponseSize) {
//                throw new IOException("Bad response size: " + responseBuffer.remaining());
//            }
//
//            int version = responseBuffer.get(0);
//            if (version != 0) {
//                throw new IOException("Bad response version: " + version);
//            }
//
//            int opcode = responseBuffer.get(1) & 0xFF;
//            if (opcode != successfulResponseOpcode) {
//                throw new IOException("Bad response opcode: " + opcode);
//            }
//
//            int resultCode = responseBuffer.getShort(2) & 0xFFFF;
//            switch (resultCode) {
//                case 0:
//                    break;
//                case 1:
//                    throw new IOException("Unsupported Version");
//                case 2:
//                    throw new IOException("Not Authorized/Refused");
//                case 3:
//                    throw new IOException("Network Failure");
//                case 4:
//                    throw new IOException("Out of resources");
//                case 5:
//                    throw new IOException("Unsupported opcode");
//                default:
//                    throw new IOException("Unrecognized result code: " + resultCode);
//            }
//
//            // the response passed sanity checks, return
//            return; // NOPMD
//        }
//
//        // exhausted number of send attempts, throw exception
//        throw new IOException("No response");
//    }
}
