package com.offbynull.peernetic.router.pcp;

import com.offbynull.peernetic.common.utils.ByteBufferUtils;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

public class MapPcpRequest extends PcpRequest {

    private ByteBuffer mappingNonce;
    private int protocol;
    private int internalPort;
    private int suggestedExternalPort;
    private InetAddress suggestedExternalIpAddress;

    public MapPcpRequest(ByteBuffer mappingNonce, int protocol, int internalPort, int suggestedExternalPort,
            InetAddress suggestedExternalIpAddress, long lifetime, PcpOption ... options) {
        super(1, lifetime, options);
        
        Validate.notNull(mappingNonce);
        Validate.isTrue(mappingNonce.remaining() == 12);
        Validate.inclusiveBetween(0, 255, protocol);
        Validate.inclusiveBetween(0, 65535, internalPort);
        Validate.inclusiveBetween(0, 65535, suggestedExternalPort);
        Validate.notNull(suggestedExternalIpAddress);
        
        if (protocol == 0) {
            Validate.isTrue(internalPort == 0);
        }
        
        if (internalPort == 0) {
            Validate.isTrue(super.getLifetime() == 0L);
        }

        this.mappingNonce = ByteBufferUtils.copyContents(mappingNonce).asReadOnlyBuffer();
        this.protocol = protocol;
        this.internalPort = internalPort;
        this.suggestedExternalPort = suggestedExternalPort;
        this.suggestedExternalIpAddress = suggestedExternalIpAddress; // for any ipv4 must be ::ffff:0:0, for any ipv6 must be ::
    }
    
    @Override
    protected void dumpOpCodeSpecificInformation(ByteBuffer dst) {
        dst.put(mappingNonce.asReadOnlyBuffer());
        dst.put((byte) protocol);
        
        for (int i = 0; i < 3; i++) { // reserved block
            dst.put((byte) 0);
        }
        
        dst.putShort((short) internalPort);
        dst.putShort((short) suggestedExternalPort);
        dst.put(PcpUtils.convertToIpv6Array(suggestedExternalIpAddress));
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
    
}