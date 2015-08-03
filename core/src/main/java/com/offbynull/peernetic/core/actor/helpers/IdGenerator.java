/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.core.actor.helpers;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.Validate;

/**
 * Generates string identifiers that are not easily predicted and guaranteed to be unique (never replicate). Generated identifiers are
 * a Base64 string comprised of randomly generated data prepended on to an unbounded counter. The random data is generated using a
 * {@link SecureRandom} set to use SHA1PRNG/SUN. The counter is {@link BigInteger} initialized to a random 4 byte value.
 * <p>
 * Note that since the counter is unbounded, generated identifiers will grow in size as more identifiers are generated. The rate of growth,
 * however, is logarithmic. That is, unless you're generating identifiers at a rate of 1 per second for multiple decades, you can be
 * confident that identifiers are never going to be more than 12 to 16 characters.
 * <p>
 * It's safe to use this class to generate unique source address suffixes for {@link RequestSubcoroutine}/{@link MultiRequestSubcoroutine}/
 * etc.  The randomness portion ensures that others can't easily predict what the next id will be and the counter portion ensures that the
 * id won't ever be duplicated by the same generator.
 * <p>
 * This class is not thread-safe / immutable.
 * @author Kasra Faghihi
 */
public final class IdGenerator {

    /**
     * Minimum size of seed (in bytes).
     */
    public static final int MIN_SEED_SIZE = 20; // http://stackoverflow.com/questions/23507657/securerandom-setseedbyte
    private static final String ALGORITHM = "SHA1PRNG";
    private static final String PROVIDER = "SUN";
    
    private BigInteger counter;
    private final ByteBuffer tempBuffer;
    private final SecureRandom random;
    
    /**
     * Construct a {@link IdGenerator} instance.
     * @param sha1PrngSeed seed to use for random number generation (must be at least {@link #MIN_SEED_SIZE} or greater in size)
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code sha1PrngSeed.length < MIN_SEED_SIZE} 
     */
    public IdGenerator(byte[] sha1PrngSeed) {
        Validate.notNull(sha1PrngSeed);
        Validate.isTrue(sha1PrngSeed.length >= MIN_SEED_SIZE);
    
        tempBuffer = ByteBuffer.allocate(Integer.BYTES); // 4 = size of int
        try {
            random = SecureRandom.getInstance(ALGORITHM, PROVIDER);
        } catch (NoSuchAlgorithmException | NoSuchProviderException ex) {
            throw new IllegalStateException(ex);
        }
        
        // you want to setseed before generating values with this securerandom, otherwise it'll try to seed itself
        // with data from potentially a blocking source, and since this is intended for use within an actor, we
        // never want to block
        random.setSeed(sha1PrngSeed);
        
        int initialCounterVal = random.nextInt() ^ random.nextInt(); // mix first with second, just in case if there's a security issue
                                                                     // or something with some other entity (e.g. a node in the network
                                                                     // that you connect to) knowing what the first randomly generated
                                                                     // long is and being able to guess what a subsequent long may be
                                                                     // (there probably isn't)
        counter = new BigInteger(Integer.toString(initialCounterVal));
    }

    /**
     * Reseeds the random number generator used by this generator.
     * @param sha1PrngSeed seed to use for random number generation (must be at least {@link #MIN_SEED_SIZE} or greater in size)
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code sha1PrngSeed.length < MIN_SEED_SIZE} 
     */
    public void reseed(byte[] sha1PrngSeed) {
        Validate.notNull(sha1PrngSeed);
        Validate.isTrue(sha1PrngSeed.length >= MIN_SEED_SIZE);
        
        random.setSeed(sha1PrngSeed);
    }

    /**
     * Generate an identifier.
     * @return new identifier
     */
    public String generate() {
        counter = counter.add(BigInteger.ONE); // increment
        byte[] counterData = counter.toByteArray();
        
        int nextRand = random.nextInt();
        tempBuffer.putInt(0, nextRand);
        byte[] randData = tempBuffer.array();
        
        byte[] combinedData = new byte[counterData.length + tempBuffer.capacity()];
        System.arraycopy(randData, 0, combinedData, 0, randData.length);
        System.arraycopy(counterData, 0, combinedData, randData.length, counterData.length);
        
        return Base64.encodeBase64String(combinedData);
    }
    
}
