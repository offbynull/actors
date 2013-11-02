package com.offbynull.p2prpc.transport;

import java.net.InetSocketAddress;

public interface IncomingPacketHandler {
    /**
     * Triggered when a network packet is received by {@link UdpBase}.
     * @param from who sent the packet
     * @param data packet contents
     * @return {@code true} if the packet was consumed, {@code false} otherwise
     */
    boolean incomingPacket(InetSocketAddress from, byte[] data);
}
