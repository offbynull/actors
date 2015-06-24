package com.offbynull.peernetic.network.gateways.udp;

import java.net.InetSocketAddress;
import java.util.Arrays;
import org.apache.commons.lang3.Validate;

final class IncomingPacket {
    private final byte[] packet;
    private final InetSocketAddress sourceSocketAddress;

    public IncomingPacket(byte[] packet, InetSocketAddress sourceSocketAddress) {
        Validate.notNull(packet);
        Validate.notNull(sourceSocketAddress);
        
        this.packet = Arrays.copyOf(packet, packet.length);
        this.sourceSocketAddress = sourceSocketAddress;
    }

    public byte[] getPacket() {
        return Arrays.copyOf(packet, packet.length);
    }

    public InetSocketAddress getSourceSocketAddress() {
        return sourceSocketAddress;
    }

    @Override
    public String toString() {
        return "IncomingPacket{" + "packet=" + Arrays.toString(packet) + ", sourceSocketAddress=" + sourceSocketAddress + '}';
    }
    
}
