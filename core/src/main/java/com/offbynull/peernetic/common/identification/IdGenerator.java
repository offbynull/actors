/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.common.identification;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import org.apache.commons.lang3.Validate;

/**
 * Default {@link Id} generator.
 * @author Kasra Faghihi
 */
public final class IdGenerator {

    private final Random random;
    private final byte[] limit;
    
    /**
     * Constructs a {@link DefaultIdGenerator} object using a {@link Random} as its underlying source.
     * @param random random number generator (should be a {@link SecureRandom} instance in most cases)
     * @param limit maximum value the id generated can be
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if limit is 0
     */
    public IdGenerator(Random random, byte[] limit) {
        Validate.notNull(random);
        Validate.notNull(limit);
        Validate.isTrue(limit.length > 0 && !new BigInteger(1, limit).equals(BigInteger.ZERO));
        this.random = random;
        this.limit = Arrays.copyOf(limit, limit.length);
    }

    /**
     * Constructs a {@link DefaultIdGenerator} using {@link SecureRandom} (with SUN / SHA1PRNG implementation).
     * @param limit maximum value the id generated can be
     * @throws NoSuchAlgorithmException thrown by {@link SecureRandom#getInstance(java.lang.String, java.lang.String) }
     * @throws NoSuchProviderException thrown by {@link SecureRandom#getInstance(java.lang.String, java.lang.String) }
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if limit is 0
     */
    public IdGenerator(byte[] limit) throws NoSuchAlgorithmException, NoSuchProviderException {
        this(SecureRandom.getInstance("SHA1PRNG", "SUN"), limit);
    }

    /**
     * Generates a {@link Id}.
     * @return a {@link Id} between {@code 0} and {@code limit}
     * @throws NullPointerException if any arguments are {@code null}
     */
    public Id generate() {
        try {
            int rawLen = limit.length;
            byte[] raw = new byte[rawLen];
            random.nextBytes(raw);

            BigInteger rawBd = new BigInteger(1, raw);
            BigInteger limitBd = new BigInteger(1, limit);
            
            rawBd = rawBd.mod(limitBd);

            return new Id(rawBd.toByteArray(), limit);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
