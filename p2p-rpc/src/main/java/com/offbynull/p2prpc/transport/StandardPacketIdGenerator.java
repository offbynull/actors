package com.offbynull.p2prpc.transport;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Random;

public final class StandardPacketIdGenerator implements PacketIdGenerator {
    private Random firstRandom;
    private Random secondRandom;
    
    public StandardPacketIdGenerator() {
        SecureRandom secureRandom = new SecureRandom();
        
        firstRandom = new Random(secureRandom.nextLong());
        secondRandom = new Random(secureRandom.nextLong());
    }

    @Override
    public byte[] generate() {
        long time = System.currentTimeMillis();
        long rand = firstRandom.nextLong() ^ secondRandom.nextLong();
        
        return ByteBuffer.allocate(16).putLong(rand).putLong(time).array();
    }
}
