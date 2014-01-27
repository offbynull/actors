package com.offbynull.peernetic.router.pcp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

public final class ThirdPartyPcpOption extends PcpOption {

    private InetAddress address;
    
    public ThirdPartyPcpOption(ByteBuffer buffer) {
        super(buffer);
        Validate.isTrue(buffer.remaining() == 16);
        byte[] addrArr = new byte[16];
        buffer.get(addrArr);
        try {
            address = InetAddress.getByAddress(addrArr);
        } catch (UnknownHostException uhe) {
            throw new IllegalStateException(uhe); // should never happen
        }
    }

    public ThirdPartyPcpOption(InetAddress address) {
        super(1, toDataSection(address));
        this.address = address;
    }
    
    private static ByteBuffer toDataSection(InetAddress address) {
        Validate.notNull(address);
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.put(PcpUtils.convertToIpv6(address).getAddress());
        
        return buffer;
    }

    public InetAddress getAddress() {
        return address;
    }
    
}
