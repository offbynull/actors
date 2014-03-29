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
package com.offbynull.peernetic.overlay.common.id;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Random;
import org.apache.commons.lang3.Validate;

/**
 * Default {@link Id} generator.
 * @author Kasra Faghihi
 */
public final class IdGenerator {

    private Random random;
    
    /**
     * Constructs a {@link DefaultIdGenerator} object using a {@link Random} as its underlying source.
     * @param random random number generator (should be a {@link SecureRandom} instance in most cases)
     * @throws NullPointerException if any arguments are {@code null}
     */
    public IdGenerator(Random random) {
        Validate.notNull(random);
        this.random = random;
    }

    /**
     * Constructs a {@link DefaultIdGenerator} using {@link SecureRandom} (with SUN / SHA1PRNG implementation).
     * @throws NoSuchAlgorithmException thrown by {@link SecureRandom#getInstance(java.lang.String, java.lang.String) }
     * @throws NoSuchProviderException thrown by {@link SecureRandom#getInstance(java.lang.String, java.lang.String) }
     */
    public IdGenerator() throws NoSuchAlgorithmException, NoSuchProviderException {
        this(SecureRandom.getInstance("SHA1PRNG", "SUN"));
    }

    /**
     * Generates a {@link Id}.
     * @param limit maximum value the id can be
     * @return a {@link Id} between {@code 0} and {@code limit}
     * @throws NullPointerException if any arguments are {@code null}
     */
    public Id generate(byte[] limit) {
        Validate.notNull(limit);
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
