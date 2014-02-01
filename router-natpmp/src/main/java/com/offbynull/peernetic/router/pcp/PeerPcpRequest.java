package com.offbynull.peernetic.router.pcp;

import com.offbynull.peernetic.common.utils.ByteBufferUtils;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

public class PeerPcpRequest extends PcpRequest {

    private ByteBuffer mappingNonce;
    private int protocol;
    private int internalPort;
    private int suggestedExternalPort;
    private InetAddress suggestedExternalIpAddress;
    private int remotePeerPort;
    private InetAddress remotePeerIpAddress;
    
    public PeerPcpRequest(ByteBuffer mappingNonce, int protocol, int internalPort, int suggestedExternalPort,
            InetAddress suggestedExternalIpAddress, int remotePeerPort, InetAddress remotePeerIpAddress, long lifetime) {
        super(2, lifetime);
        
        Validate.notNull(mappingNonce);
        Validate.isTrue(mappingNonce.remaining() == 12);
        Validate.inclusiveBetween(0, 255, protocol);
        Validate.inclusiveBetween(1, 65535, internalPort); // must not be 0
        Validate.inclusiveBetween(0, 65535, suggestedExternalPort); // 0 = no preference
        Validate.notNull(suggestedExternalIpAddress);
        Validate.inclusiveBetween(1, 65535, remotePeerPort); // cannot be 0
        Validate.notNull(remotePeerIpAddress);

        this.mappingNonce = ByteBufferUtils.copyContents(mappingNonce).asReadOnlyBuffer();
        this.protocol = protocol;
        this.internalPort = internalPort;
        this.suggestedExternalPort = suggestedExternalPort;
        this.suggestedExternalIpAddress = suggestedExternalIpAddress; // for any ipv4 must be ::ffff:0:0, for any ipv6 must be ::
        this.remotePeerPort = remotePeerPort;
        this.remotePeerIpAddress = remotePeerIpAddress; // for any ipv4 must be ::ffff:0:0, for any ipv6 must be ::
    }
    
    @Override
    protected void dumpOpCodeSpecificInformation(ByteBuffer dst) {
        dst.put(mappingNonce);
        dst.put((byte) protocol);
        
        for (int i = 0; i < 3; i++) { // reserved block
            dst.put((byte) 0);
        }
        
        dst.putShort((short) internalPort);
        dst.putShort((short) suggestedExternalPort);
        dst.put(PcpUtils.convertToIpv6Array(suggestedExternalIpAddress));
        dst.putShort((short) remotePeerPort);
        
        for (int i = 0; i < 2; i++) { // reserved block
            dst.put((byte) 0);
        }
        
        dst.put(PcpUtils.convertToIpv6Array(remotePeerIpAddress));
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

    public int getSuggestedExternalPort() {
        return suggestedExternalPort;
    }

    public InetAddress getSuggestedExternalIpAddress() {
        return suggestedExternalIpAddress;
    }

    public int getRemotePeerPort() {
        return remotePeerPort;
    }

    public InetAddress getRemotePeerIpAddress() {
        return remotePeerIpAddress;
    }
    
}
