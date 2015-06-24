package com.offbynull.peernetic.network.gateways.udp;

import java.net.InetSocketAddress;
import java.util.Arrays;
import org.apache.commons.lang3.Validate;

final class OutgoingPacket {
    private final byte[] packet;
    private final InetSocketAddress destinationSocketAddress;

    public OutgoingPacket(byte[] packet, InetSocketAddress destinationSocketAddress) {
        Validate.notNull(packet);
        Validate.notNull(destinationSocketAddress);
        
        this.packet = Arrays.copyOf(packet, packet.length);
        this.destinationSocketAddress = destinationSocketAddress;
    }

    public byte[] getPacket() {
        return Arrays.copyOf(packet, packet.length);
    }

    public InetSocketAddress getDestinationSocketAddress() {
        return destinationSocketAddress;
    }

    @Override
    public String toString() {
        return "OutgoingPacket{" + "packet=" + Arrays.toString(packet) + ", destinationSocketAddress=" + destinationSocketAddress + '}';
    }
    
}
