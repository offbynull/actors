package com.offbynull.peernetic.router.pcp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

public final class PeerPcpResponse extends PcpResponse {
    private ByteBuffer mappingNonce;
    private int protocol;
    private int internalPort;
    private int assignedExternalPort;
    private InetAddress assignedExternalIpAddress;
    private int remotePeerPort;
    private InetAddress remotePeerIpAddress;

    public PeerPcpResponse(ByteBuffer buffer) {
        super(buffer);
        
        Validate.isTrue(super.getOp() == 1);
        
        Validate.inclusiveBetween(0, 255, protocol);
        Validate.inclusiveBetween(0, 65535, internalPort);
        Validate.inclusiveBetween(0, 65535, assignedExternalPort);
        Validate.notNull(assignedExternalIpAddress);

        mappingNonce = ByteBuffer.allocate(12);
        mappingNonce.put(buffer);
        mappingNonce.flip();
        mappingNonce = mappingNonce.asReadOnlyBuffer();
        this.protocol = buffer.get() & 0xFF;
        
        for (int i = 0; i < 3; i++) { // reserved block
            buffer.put((byte) 0);
        }
        
        this.internalPort = buffer.getShort() & 0xFFFF;
        this.assignedExternalPort = buffer.getShort() & 0xFFFF;
        byte[] addrArr = new byte[16];
        buffer.get(addrArr);
        try {
            this.assignedExternalIpAddress = InetAddress.getByAddress(addrArr); // should automatically shift down to ipv4 if ipv4-to-ipv6
                                                                                // mapped address
        } catch (UnknownHostException uhe) {
            throw new IllegalStateException(uhe); // should never happen, will always be 16 bytes
        }
        this.remotePeerPort = buffer.getShort() & 0xFFFF;
        
        for (int i = 0; i < 2; i++) { // reserved block
            buffer.put((byte) 0);
        }
        
        buffer.get(addrArr);
        try {
            this.remotePeerIpAddress = InetAddress.getByAddress(addrArr); // should automatically shift down to ipv4 if ipv4-to-ipv6
                                                                          // mapped address
        } catch (UnknownHostException uhe) {
            throw new IllegalStateException(uhe); // should never happen, will always be 16 bytes
        }
    }

    public ByteBuffer getMappingNonce() {
        return mappingNonce.asReadOnlyBuffer();
    }

    public int getProtocol() {
        return protocol;
    }

    public int getInternalPort() {
        return internalPort;
    }

    public int getAssignedExternalPort() {
        return assignedExternalPort;
    }

    public InetAddress getAssignedExternalIpAddress() {
        return assignedExternalIpAddress;
    }

    public int getRemotePeerPort() {
        return remotePeerPort;
    }

    public InetAddress getRemotePeerIpAddress() {
        return remotePeerIpAddress;
    }
    
}
