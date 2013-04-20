package com.offbynull.peernetic.p2ptools.identification;

import java.math.BigInteger;
import java.util.Objects;

public class LimitedId {

    private BigInteger data;
    private BigInteger limit;

    private LimitedId(BigInteger data, BigInteger limit) {
        
        this.data = data;
        this.limit = limit;
    }

    public LimitedId(byte[] data, byte[] limit) {
        if (data == null || limit == null) {
            throw new NullPointerException();
        }
        
        this.data = new BigInteger(1, data);
        this.limit = new BigInteger(1, limit); 
        
        if (this.data.compareTo(this.limit) > 0) {
            throw new IllegalArgumentException();
        }
    }
    
    public LimitedId add(LimitedId other) {
        if (other == null) {
            throw new NullPointerException();
        }
        if (!limit.equals(other.limit)) {
            throw new IllegalArgumentException();
        }

        BigInteger added = data.add(other.data);
        if (added.compareTo(limit) > 0) {
            added = added.subtract(limit).subtract(BigInteger.ONE);
        }

        LimitedId addedId = new LimitedId(added, limit);

        return addedId;
    }

    public LimitedId subtract(LimitedId other) {
        if (other == null) {
            throw new NullPointerException();
        }
        if (!limit.equals(other.limit)) {
            throw new IllegalArgumentException();
        }

        BigInteger subtracted = data.subtract(other.data);
        if (subtracted.signum() == -1) {
            subtracted = subtracted.add(limit).add(BigInteger.ONE);
        }

        LimitedId subtractedId = new LimitedId(subtracted, limit);

        return subtractedId;
    }

    public int comparePosition(LimitedId base, LimitedId other) {
        if (base == null || other == null) {
            throw new NullPointerException();
        }
        if (!limit.equals(base.limit) || !limit.equals(other.limit)) {
            throw new IllegalArgumentException();
        }
        
        LimitedId otherOffsetId = other.subtract(base);
        LimitedId offsetId = this.subtract(base);

        BigInteger otherOffsetIdNum = otherOffsetId.asBigInteger();
        BigInteger offsetIdNum = offsetId.asBigInteger();
        
        return offsetIdNum.compareTo(otherOffsetIdNum);
    }

    public boolean isWithin(LimitedId lower, boolean lowerInclusive,
            LimitedId upper, boolean upperInclusive) {
        if (lower == null || upper == null) {
            throw new NullPointerException();
        }
        
        return isWithin(lower, lower, lowerInclusive, upper, upperInclusive);
    }
    
    public boolean isWithin(LimitedId base, LimitedId lower, boolean lowerInclusive,
            LimitedId upper, boolean upperInclusive) {
        if (lower == null || upper == null || base == null) {
            throw new NullPointerException();
        }
        
        if (!limit.equals(base.limit) || !limit.equals(lower.limit)
                || !limit.equals(upper.limit)) {
            throw new IllegalArgumentException();
        }
        
        if (lowerInclusive && upperInclusive) {
            return comparePosition(base, lower) >= 0
                    && comparePosition(base, upper) <= 0;
        } else if (lowerInclusive) {
            return comparePosition(base, lower) >= 0
                    && comparePosition(base, upper) < 0;
        } else if (upperInclusive) {
            return comparePosition(base, lower) > 0
                    && comparePosition(base, upper) <= 0;            
        } else {
            return comparePosition(base, lower) > 0
                    && comparePosition(base, upper) < 0;
        }
    }
    
    public BigInteger asBigInteger() {
        return data;
    }

    public byte[] asByteArray() {
        return data.toByteArray();
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
        final LimitedId other = (LimitedId) obj;
        if (!Objects.equals(this.data, other.data)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "BitLimitedId{" + "data=" + data + ", limit=" + limit + '}';
    }
}
