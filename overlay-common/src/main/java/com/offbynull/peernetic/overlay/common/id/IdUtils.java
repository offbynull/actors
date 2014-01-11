/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
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
import java.util.Random;
import org.apache.commons.lang3.Validate;

/**
 * Utilities for {@link Id}.
 * @author Kasra Faghihi
 */
public final class IdUtils {

    private IdUtils() {
        // do nothing
    }

    /**
     * Validates that the limits are the same for two {@link Pointer}s.
     * @param <A> address type
     * @param ptr1 pointer 1
     * @param ptr2 pointer 2
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if the limits aren't equal
     */
    public static <A> void ensureLimitsMatch(Pointer<A> ptr1, Pointer<A> ptr2) {
        Validate.notNull(ptr1);
        Validate.notNull(ptr2);
        Validate.isTrue(ptr1.getId().getLimitAsBigInteger().equals(ptr2.getId().getLimitAsBigInteger()));
    }

    /**
     * Validates that the limits are the same for two {@link Id}s.
     * @param id1 id 1
     * @param id2 id 2
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if the limits aren't equal
     */
    public static void ensureLimitsMatch(Id id1, Id id2) {
        Validate.notNull(id1);
        Validate.notNull(id2);
        Validate.isTrue(id1.getLimitAsBigInteger().equals(id2.getLimitAsBigInteger()));
    }
    
    /**
     * Validates that a {@link Pointer}'s limit matches {@code 2^n-1}. In otherwords, ensures that all bits making up the limit are 1.
     * @param ptr pointer
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if the limit doesn't match {@code 2^n-1}
     */
    public static void ensureLimitPowerOfTwo(Pointer<?> ptr) {
        Validate.notNull(ptr);
        ensureLimitPowerOfTwo(ptr.getId());
    }
    
    /**
     * Gets the bit length of an {@link Pointer}'s limit.
     * @param ptr pointer
     * @return bit length of {@code id}'s limit
     * @throws NullPointerException if any argument is {@code null}
     */
    public static int getLimitBitLength(Pointer<?> ptr) {
        Validate.notNull(ptr);
        return getLimitBitLength(ptr.getId());
    }

    /**
     * Validates that a {@link Id}'s limit matches {@code 2^n-1}. In otherwords, ensures that all bits making up the limit are 1.
     * @param id id
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if the limit  doesn't match {@code 2^n-1}
     */
    public static void ensureLimitPowerOfTwo(Id id) {
        Validate.notNull(id);
        
        BigInteger limit = id.getLimitAsBigInteger();
        int bitLength = limit.bitLength();
        
        for (int i = 0; i < bitLength; i++) {
            if (!limit.testBit(i)) {
                throw new IllegalArgumentException();
            }
        }
    }
    
    /**
     * Gets the bit length of an {@link Id}'s limit.
     * @param id id
     * @return bit length of {@code id}'s limit
     * @throws NullPointerException if any argument is {@code null}
     */
    public static int getLimitBitLength(Id id) {
        Validate.notNull(id);
        
        return id.getLimitAsBigInteger().bitLength();
    }
    
    /**
     * Generates a limit that's {@code 2^n-1}. Another way to think of it is, generates a limit that successively {@code n = (n << 1) | 1}
     * for {@code n} times -- making sure you have a limit that's value is all 1 bits.
     * <p/>
     * Examples:
     * <ul>
     * <li>{@code n = 0, limit = 0 (0b)}<li/>
     * <li>{@code n = 1, limit = 1 (1b)}<li/>
     * <li>{@code n = 2, limit = 3 (11b)}<li/>
     * <li>{@code n = 4, limit = 7 (111b)}<li/>
     * <li>{@code n = 8, limit = 15 (1111b)}<li/>
     * </ul>
     * @param exp exponent, such that the returned value is {@code 2^exp-1}
     * @return {@code 2^exp-1} as a byte array
     * @throws IllegalArgumentException if {@code exp} is negative
     */
    public static byte[] generatePowerOfTwoLimit(int exp) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, exp);

        BigInteger val = BigInteger.ZERO;
        
        for (int i = 0; i < exp; i++) {
            val = val.shiftLeft(1).or(BigInteger.ONE);
        }
        
        return val.toByteArray();
    }
    
    /**
     * Generates a random id.
     * @param random random
     * @param limit limit of id
     * @return random id within {@code limit}
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if limit is {@code 0}
     */
    public static Id generateRandomId(Random random, byte[] limit) {
        Validate.notNull(random);
        Validate.notNull(limit);
        
        BigInteger limitBigInteger = new BigInteger(limit);
        Validate.isTrue(!limitBigInteger.equals(BigInteger.ZERO));
        
        byte[] value = new byte[limit.length];
        random.nextBytes(value);
        
        BigInteger valueBigInteger = new BigInteger(value);
        BigInteger tapperedValueBigInteger = valueBigInteger.mod(limitBigInteger);
        
        return new Id(tapperedValueBigInteger.toByteArray(), limit);
    }
    
    /**
     * Generates a random id that's inclusively between {@code 0} and {@code 2^n-1}.
     * @param random random
     * @param exp exponent, such that the returned id is between {@code 0} and {@code 2^exp-1}
     * @return an id between {@code 0} and {@code 2^exp-1} as a byte array
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if {@code exp} is negative
     */
    public static Id generateRandomPowerOfTwoId(Random random, int exp) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, exp);

        BigInteger val = BigInteger.ZERO;
        
        for (int i = 0; i < exp; i++) {
            val = val.shiftLeft(1).or(random.nextBoolean() ? BigInteger.ONE : BigInteger.ZERO);
        }
        
        return new Id(val.toByteArray(), generatePowerOfTwoLimit(exp));
    }
}
