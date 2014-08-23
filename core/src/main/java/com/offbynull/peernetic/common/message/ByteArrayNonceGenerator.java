package com.offbynull.peernetic.common.message;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import org.apache.commons.lang3.Validate;

public final class ByteArrayNonceGenerator implements NonceGenerator<byte[]> {

    private Random random;
    private int size;

    public ByteArrayNonceGenerator(int size) {
        Validate.isTrue(size > 0);
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException nsae) {
            throw new IllegalStateException(nsae);
        }
        this.size = size;
    }
    
    public ByteArrayNonceGenerator(Random random, int size) {
        Validate.isTrue(size > 0);
        
        Validate.notNull(random);
        this.random = random;
        this.size = size;
    }
    
    @Override
    public Nonce<byte[]> generate() {
        byte[] value = new byte[size];
        random.nextBytes(value);
        
        return new ByteArrayNonce(value);
    }
    
}
