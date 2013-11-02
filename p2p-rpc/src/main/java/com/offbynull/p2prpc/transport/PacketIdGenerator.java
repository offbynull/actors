package com.offbynull.p2prpc.transport;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Random;

public final class PacketIdGenerator {
    private Random firstRandom;
    private Random secondRandom;
    
    public PacketIdGenerator() {
        SecureRandom secureRandom = new SecureRandom();
        
        firstRandom = new Random(secureRandom.nextLong());
        secondRandom = new Random(secureRandom.nextLong());
    }

    public byte[] generate() {
        long time = System.currentTimeMillis();
        long rand = firstRandom.nextLong() ^ secondRandom.nextLong();
        
        return ByteBuffer.allocate(16).putLong(rand).putLong(time).array();
    }
}
