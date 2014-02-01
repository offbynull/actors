package com.offbynull.peernetic.router.pcp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

public final class MapPcpResponse extends PcpResponse {
    private ByteBuffer mappingNonce;
    private int protocol;
    private int internalPort;
    private int assignedExternalPort;
    private InetAddress assignedExternalIpAddress;

    public MapPcpResponse(ByteBuffer buffer) {
        super(buffer);
        
        Validate.isTrue(super.getOp() == 1);

        mappingNonce = ByteBuffer.allocate(12);
        buffer.get(mappingNonce.array());
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
        
        Validate.inclusiveBetween(0, 255, protocol);
        Validate.inclusiveBetween(1, 65535, internalPort);
        Validate.inclusiveBetween(1, 65535, assignedExternalPort);
        Validate.notNull(assignedExternalIpAddress);
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
    
}
