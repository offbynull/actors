package com.offbynull.p2prpc.session;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Random;

public final class MessageIdGenerator {
    private Random random;
    
    public MessageIdGenerator() {
        SecureRandom secureRandom = new SecureRandom();
        
        random = new Random(secureRandom.nextLong());
    }

    public MessageId generate() {
        long time = System.currentTimeMillis();
        long rand = random.nextLong();
        
        // This is not secure. Already worked out a method where you may be able to work backwards to the seed if you have enough rand
        // values.
        
        return new MessageId(ByteBuffer.allocate(16).putLong(rand).putLong(time).array());
    }
}
