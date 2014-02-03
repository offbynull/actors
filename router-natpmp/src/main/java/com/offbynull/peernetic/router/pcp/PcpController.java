package com.offbynull.peernetic.router.pcp;

import com.offbynull.peernetic.router.common.BadResponseException;
import com.offbynull.peernetic.router.common.NoResponseException;
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

public final class PcpController implements Closeable {
    private PcpCommunicator communicator;
    private InetAddress selfAddress;
    private int sendAttempts;
    private Random random;

    public PcpController(Random random, InetAddress gatewayAddress, InetAddress selfAddress, int sendAttempts,
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
    
    public AnnouncePcpResponse announce() throws IOException, InterruptedException {
        AnnouncePcpRequest req = new AnnouncePcpRequest();
        
        AnnounceResponseCreator creator = new AnnounceResponseCreator();
        return performRequest(req, creator);
    }
    
    public MapPcpResponse createInboundMapping(PortType portType, int internalPort, int suggestedExternalPort,
            InetAddress suggestedExternalIpAddress, long lifetime, PcpOption ... options) throws IOException, InterruptedException {
        byte[] nonce = new byte[12];
        random.nextBytes(nonce);
        
        MapPcpRequest req = new MapPcpRequest(ByteBuffer.wrap(nonce), portType.getProtocolNumber(), internalPort, suggestedExternalPort,
                suggestedExternalIpAddress, lifetime, options);

        MapResponseCreator creator = new MapResponseCreator(req);
        return performRequest(req, creator);
    }
    
    public PeerPcpResponse createOutboundMapping(PortType portType, int internalPort, int suggestedExternalPort,
            InetAddress suggestedExternalIpAddress, int remotePeerPort, InetAddress remotePeerIpAddress, long lifetime,
            PcpOption ... options) throws IOException, InterruptedException {
        byte[] nonce = new byte[12];
        random.nextBytes(nonce);
        
        PeerPcpRequest req = new PeerPcpRequest(ByteBuffer.wrap(nonce), portType.getProtocolNumber(), internalPort, suggestedExternalPort,
                suggestedExternalIpAddress, remotePeerPort, remotePeerIpAddress, lifetime, options);

        PeerResponseCreator creator = new PeerResponseCreator(req);
        return performRequest(req, creator);
    }

    private <T extends PcpResponse> T performRequest(PcpRequest request, Creator<T> creator) throws IOException, InterruptedException {
        ByteBuffer sendBuffer = ByteBuffer.allocate(1100);
            
        request.dump(sendBuffer, selfAddress);
        sendBuffer.flip();
        
        for (int i = 1; i <= sendAttempts; i++) {
            T response = attemptRequest(sendBuffer, i, creator);
            if (response != null) {
                return response;
            }
        }
        
        throw new NoResponseException();
    }
    
    private <T extends PcpResponse> T attemptRequest(ByteBuffer sendBuffer, int attempt, Creator<T> creator)
            throws IOException, InterruptedException {
        
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
                throw new BadResponseException(e);
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
                throw new BadResponseException(e);
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
                throw new BadResponseException(e);
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
