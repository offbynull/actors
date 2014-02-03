package com.offbynull.peernetic.router.pcp;

import java.nio.ByteBuffer;

public interface PcpResponseListener {
    void incomingPacket(CommunicationType type, ByteBuffer packet);
    
    public enum CommunicationType {
        UNICAST,
        MULTICAST
    }
}
