package com.offbynull.peernetic.router.pcp;

import com.offbynull.peernetic.router.common.NoResponseException;
import com.offbynull.peernetic.router.common.PortType;
import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Random;
import org.apache.commons.lang3.Validate;

public final class PcpController implements Closeable {
    private final DatagramSocket socket;
    private final InetAddress gatewayAddress;
    private final InetAddress selfAddress;
    private final int sendAttempts;
    private final Random random;

    public PcpController(InetAddress gatewayAddress, InetAddress selfAddress, int sendAttempts) throws IOException {
        Validate.notNull(gatewayAddress);
        Validate.notNull(selfAddress);
        Validate.inclusiveBetween(1, 9, sendAttempts);
        this.socket = new DatagramSocket(0);
        this.gatewayAddress = gatewayAddress;
        this.selfAddress = selfAddress;
        this.sendAttempts = sendAttempts;
        this.random = new Random();
    }
    
    public AnnouncePcpResponse announce() throws IOException {
        AnnouncePcpRequest req = new AnnouncePcpRequest();
        
        AnnounceResponseCreator creator = new AnnounceResponseCreator();
        return performRequest(req, creator);
    }
    
    public MapPcpResponse createInboundMapping(PortType portType, int internalPort, int suggestedExternalPort,
            InetAddress suggestedExternalIpAddress, long lifetime) throws IOException {
        byte[] nonce = new byte[12];
        random.nextBytes(nonce);
        
        MapPcpRequest req = new MapPcpRequest(ByteBuffer.wrap(nonce), portType.getProtocolNumber(), internalPort, suggestedExternalPort,
                suggestedExternalIpAddress, lifetime);

        MapResponseCreator creator = new MapResponseCreator(req);
        return performRequest(req, creator);
    }
    
    public PeerPcpResponse createOutboundMapping(PortType portType, int internalPort, int suggestedExternalPort,
            InetAddress suggestedExternalIpAddress, int remotePeerPort, InetAddress remotePeerIpAddress, long lifetime) throws IOException {
        byte[] nonce = new byte[12];
        random.nextBytes(nonce);
        
        PeerPcpRequest req = new PeerPcpRequest(ByteBuffer.wrap(nonce), portType.getProtocolNumber(), internalPort, suggestedExternalPort,
                suggestedExternalIpAddress, remotePeerPort, remotePeerIpAddress, lifetime);

        PeerResponseCreator creator = new PeerResponseCreator(req);
        return performRequest(req, creator);
    }

    private <T extends PcpResponse> T performRequest(PcpRequest request, Creator<T> creator) throws IOException {
        ByteBuffer sendBuffer = ByteBuffer.allocate(1100);
        ByteBuffer recvBuffer = ByteBuffer.allocate(1100);
            
        request.dump(sendBuffer, selfAddress);
        sendBuffer.flip();

        for (int i = 1; i <= sendAttempts; i++) {
            T response = attemptRequest(socket, sendBuffer, recvBuffer, i, creator);
            if (response != null) {
                return response;
            }
        }
        
        throw new NoResponseException();
    }
    
    private <T extends PcpResponse> T attemptRequest(DatagramSocket socket, ByteBuffer sendBuffer, ByteBuffer recvBuffer, int attempt,
            Creator<T> creator) throws IOException {
        
        DatagramPacket request = new DatagramPacket(sendBuffer.array(), sendBuffer.limit(), gatewayAddress, 5351);
        socket.send(request);
        
        // timeout duration should double each iteration, starting from 250 according to spec
        // i = 1, maxWaitTime = (1 << (1-1)) * 250 = (1 << 0) * 250 = 1 * 250 = 250
        // i = 2, maxWaitTime = (1 << (2-1)) * 250 = (1 << 1) * 250 = 2 * 250 = 500
        // i = 3, maxWaitTime = (1 << (3-1)) * 250 = (1 << 2) * 250 = 4 * 250 = 1000
        // i = 4, maxWaitTime = (1 << (4-1)) * 250 = (1 << 3) * 250 = 8 * 250 = 2000
        // ...
        int maxWaitTime = (1 << (attempt - 1)) * 250; // NOPMD
        
        T pcpResponse = null;
        
        long endTime = System.currentTimeMillis() + maxWaitTime;
        long waitTime;
        while ((waitTime = endTime - System.currentTimeMillis()) > 0L) {
            waitTime = Math.max(waitTime, 0L); // must be at least 0, probably should never happen
            
            socket.setSoTimeout((int) waitTime);

            DatagramPacket response = new DatagramPacket(recvBuffer.array(), recvBuffer.capacity());
            try {
                socket.receive(response);
            } catch (SocketTimeoutException ste) {
                break;
            }

            if (!response.getAddress().equals(gatewayAddress)) { // data isn't from our gateway, ignore
                continue;
            }
            
            recvBuffer.limit(response.getLength());
            
            pcpResponse = creator.create(recvBuffer);
            if (pcpResponse != null) {
                break;
            }
        }
        
        return pcpResponse;
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
    
    private interface Creator<T extends PcpResponse> {
        T create(ByteBuffer response);
    }

    private static final class AnnounceResponseCreator implements Creator<AnnouncePcpResponse> {
        public AnnounceResponseCreator() {
        }

        @Override
        public AnnouncePcpResponse create(ByteBuffer recvBuffer) {
            try {
                AnnouncePcpResponse response = new AnnouncePcpResponse(recvBuffer);
                
                return response;
            } catch (Exception e) { // NOPMD
                // do nothing
            }
            
            return null;
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
            try {
                MapPcpResponse response = new MapPcpResponse(recvBuffer);
                
                if (response.getMappingNonce().equals(request.getMappingNonce()) &&
                        response.getProtocol() == request.getProtocol() &&
                        response.getInternalPort() == request.getInternalPort()) {
                    return response;
                }
            } catch (Exception e) { // NOPMD
                // do nothing
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
            try {
                PeerPcpResponse response = new PeerPcpResponse(recvBuffer);
                
                if (response.getMappingNonce().equals(request.getMappingNonce()) &&
                        response.getProtocol() == request.getProtocol() &&
                        response.getRemotePeerPort() == request.getRemotePeerPort() &&
                        response.getRemotePeerIpAddress().equals(request.getRemotePeerIpAddress())) {
                    return response;
                }
            } catch (Exception e) { // NOPMD
                // do nothing
            }
            
            return null;
        }
    }
}
