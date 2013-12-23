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
import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * An ID between 0 and some pre-defined limit.
 * @author Kasra Faghihi
 */
public final class Id {

    private BigInteger data;
    private BigInteger limit;

    /**
     * Constructs a {@link Id} from {@link BigInteger}s.
     * @param data id value
     * @param limit limit value
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code data > limit}, or if either argument is {@code < 0}
     */
    private Id(BigInteger data, BigInteger limit) {
        Validate.notNull(data);
        Validate.notNull(limit);
        Validate.isTrue(data.compareTo(limit) <= 0 && data.signum() >= 0 && limit.signum() >= 0);
        
        this.data = data;
        this.limit = limit;
    }

    /**
     * Constructs a {@link Id} from byte arrays. It's assumed that the byte arrays contain unsigned values and are aligned to byte
     * boundaries (e.g. if you want to use the 6 bits instead of a whole byte (8 bits), make sure the 2 top-most bits are 0).
     * @param data id value
     * @param limit limit value
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code data > limit}
     */
    public Id(byte[] data, byte[] limit) {
        this(new BigInteger(1, data), new BigInteger(1, limit)); 
    }
    
    /**
     * Adds two IDs together. {@code this} is the left-hand side. The limit of the IDs must match.
     * @param rhs right-hand side
     * @return {@code lhs + rhs}, wrapped if it exceeds the limit
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if the limit from {@code lhs} doesn't match the limit from {@code rhs}
     */
    public Id add(Id rhs) {
        Id lhs = this;
        
        Validate.notNull(lhs);
        Validate.notNull(rhs);
        Validate.isTrue(lhs.limit.equals(rhs.limit));

        BigInteger added = lhs.data.add(rhs.data);
        if (added.compareTo(lhs.limit) > 0) {
            added = added.subtract(lhs.limit).subtract(BigInteger.ONE);
        }

        Id addedId = new Id(added, lhs.limit);

        return addedId;
    }

    /**
     * Subtracts two IDs. {@code this} is the left-hand side. The limit of the IDs must match.
     * @param rhs right-hand side
     * @return {@code lhs + rhs}, wrapped around limit if it goes below {@code 0}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if the limit from {@code lhs} doesn't match the limit from {@code rhs}
     */
    public Id subtract(Id rhs) {
        Id lhs = this;
        
        Validate.notNull(lhs);
        Validate.notNull(rhs);
        Validate.isTrue(lhs.limit.equals(rhs.limit));

        BigInteger subtracted = lhs.data.subtract(rhs.data);
        if (subtracted.signum() == -1) {
            subtracted = subtracted.add(lhs.limit).add(BigInteger.ONE);
        }

        Id subtractedId = new Id(subtracted, lhs.limit);

        return subtractedId;
    }

    /**
     * Compare the position of two IDs, using a base reference point. {@code this} is the left-hand side. Another way to think of this is
     * that this method compares two IDs from the view of a certain reference point.
     * <p/>
     * <b>Example 1:</b><br/>
     * The ID limit is 16<br/>
     * {@code lhs = 15}<br/>
     * {@code rhs = 2}<br/>
     * {@code base = 10}<br/>
     * In this case, {@code lhs > rhs}. From {@code base}'s view, {@code lhs} is only 5 nodes away, while {@code rhs} is 8 nodes away.
     * <p/>
     * <b>Example 2:</b><br/>
     * The ID limit is 16<br/>
     * {@code lhs = 9}<br/>
     * {@code rhs = 2}<br/>
     * {@code base = 10}<br/>
     * In this case, {@code rhs < lhs}. From {@code base}'s view, {@code lhs} is 15 nodes away, while {@code rhs} is 8 only nodes away.
     * @param base reference point
     * @param rhs right-hand side
     * @return -1, 0 or 1 as {@lhs} is less than, equal to, or greater than {@code rhs}.
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if the limit from {@code lhs} or {@code rhs} doesn't match the limit from {@code base}
     */
    public int comparePosition(Id base, Id rhs) {
        Id lhs = this;
        
        Validate.notNull(base);
        Validate.notNull(lhs);
        Validate.notNull(rhs);
        Validate.isTrue(base.limit.equals(lhs.limit) && base.limit.equals(rhs.limit));
        
        Id lhsOffsetId = lhs.subtract(base);
        Id rhsOffsetId = rhs.subtract(base);

        BigInteger rhsOffsetIdNum = rhsOffsetId.getValueAsBigInteger();
        BigInteger lhsOffsetIdNum = lhsOffsetId.getValueAsBigInteger();
        
        return lhsOffsetIdNum.compareTo(rhsOffsetIdNum);
    }

    /**
     * Checks to see if this ID is between two other IDs. Equivalent to calling
     * {@link #isWithin(com.offbynull.peernetic.overlay.common.id.Id, com.offbynull.peernetic.overlay.common.id.Id, boolean,
     * com.offbynull.peernetic.overlay.common.id.Id, boolean) } where the {@code base} argument and the {@code lower} argument are
     * the same.
     * @param lower lower ID bound
     * @param lowerInclusive {@code true} if lower ID bound is inclusive, {@code false} if exclusive
     * @param upper upper ID bound
     * @param upperInclusive {@code true} if upper ID bound is inclusive, {@code false} if exclusive
     * @return {@code true} if this ID is between the two specified values
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if ID limits don't match this ID's limit
     */
    public boolean isWithin(Id lower, boolean lowerInclusive, Id upper, boolean upperInclusive) {
        Validate.notNull(lower);
        Validate.notNull(upper);
        Validate.isTrue(limit.equals(lower.limit) && limit.equals(upper.limit));
        
        if (lowerInclusive && upperInclusive) {
            return comparePosition(lower, lower) >= 0 && comparePosition(lower, upper) <= 0;
        } else if (lowerInclusive) {
            return comparePosition(lower, lower) >= 0 && comparePosition(lower, upper) < 0;
        } else if (upperInclusive) {
            return comparePosition(lower, lower) > 0 && comparePosition(lower, upper) <= 0;            
        } else {
            return comparePosition(lower, lower) > 0 && comparePosition(lower, upper) < 0;
        }
    }
    
    /**
     * Returns this ID as a {@link BigInteger}.
     * @return this ID as a {@link BigInteger}
     */
    public BigInteger getValueAsBigInteger() {
        return data;
    }

    /**
     * Returns this ID as a byte array.
     * @return this ID as a byte array
     */
    public byte[] getValueAsByteArray() {
        return data.toByteArray();
    }

    /**
     * Returns this ID's limit as a {@link BigInteger}.
     * @return this ID's limit as a {@link BigInteger}
     */
    public BigInteger getLimitAsBigInteger() {
        return limit;
    }

    /**
     * Returns this ID's limit as a byte array.
     * @return this ID's limit as a byte array
     */
    public byte[] getLimitAsByteArray() {
        return limit.toByteArray();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.data);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Id other = (Id) obj;
        if (!Objects.equals(this.data, other.data)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Id{" + "data=" + data + ", limit=" + limit + '}';
    }
}
