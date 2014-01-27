package com.offbynull.peernetic.router.pcp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

public final class FilterPcpOption extends PcpOption {
    private int prefixLength;
    private int remotePeerPort;
    private InetAddress address;
    
    public FilterPcpOption(ByteBuffer buffer) {
        super(buffer);
        
        buffer.get(); // reserved
        prefixLength = buffer.get() & 0xFF;
        remotePeerPort = buffer.getShort() & 0xFFFF;
        
        Validate.inclusiveBetween(0, 128, prefixLength); // 0 indicates 'no filter'
        Validate.inclusiveBetween(0, 65535, remotePeerPort); // 0 indicates 'all ports'
        
        Validate.isTrue(buffer.remaining() == 16);
        byte[] addrArr = new byte[16];
        buffer.get(addrArr);
        try {
            address = InetAddress.getByAddress(addrArr);
        } catch (UnknownHostException uhe) {
            throw new IllegalStateException(uhe); // should never happen
        }
    }
    
    public FilterPcpOption(int prefixLength, int remotePeerPort, InetAddress address) {
        super(3, toDataSection(prefixLength, remotePeerPort, address));
        
        this.prefixLength = prefixLength;
        this.remotePeerPort = remotePeerPort;
        this.address = address;
    }
    
    private static ByteBuffer toDataSection(int prefixLength, int remotePeerPort, InetAddress address) {
        Validate.inclusiveBetween(0, 128, prefixLength); // 0 indicates 'no filter'
        Validate.inclusiveBetween(0, 65535, remotePeerPort); // 0 indicates 'all ports'
        Validate.notNull(address);
        
        ByteBuffer buffer = ByteBuffer.allocate(20);
        buffer.put((byte) 0); // reserved
        buffer.put((byte) prefixLength);
        buffer.putShort((short) remotePeerPort);
        buffer.put(PcpUtils.convertToIpv6(address).getAddress());
        
        return buffer;
    }

    public int getPrefixLength() {
        return prefixLength;
    }

    public int getRemotePeerPort() {
        return remotePeerPort;
    }

    public InetAddress getAddress() {
        return address;
    }
    
}
