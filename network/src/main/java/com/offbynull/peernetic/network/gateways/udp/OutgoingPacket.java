package com.offbynull.peernetic.network.gateways.udp;

import java.net.InetSocketAddress;
import java.util.Arrays;
import org.apache.commons.lang3.Validate;

final class OutgoingPacket {
    private final byte[] packet;
    private final InetSocketAddress destination;

    public OutgoingPacket(byte[] packet, InetSocketAddress destination) {
        Validate.notNull(packet);
        Validate.notNull(destination);
        
        this.packet = Arrays.copyOf(packet, packet.length);
        this.destination = destination;
    }

    public byte[] getPacket() {
        return Arrays.copyOf(packet, packet.length);
    }

    public InetSocketAddress getDestination() {
        return destination;
    }
    
}
