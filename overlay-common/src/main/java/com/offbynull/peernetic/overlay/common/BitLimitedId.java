package com.offbynull.peernetic.overlay.common;

import java.math.BigInteger;
import java.util.Objects;

public class BitLimitedId {

    private BigInteger data;
    private BigInteger limit;
    private int bitCount;

    private BitLimitedId(int bitCount, BigInteger data, BigInteger limit) {
        this.data = data;
        this.limit = limit;
        this.bitCount = bitCount;
    }

    public BitLimitedId(int bitCount, byte[] raw) {
        if (raw == null) {
            throw new NullPointerException();
        }

        if (bitCount <= 0) {
            throw new IllegalArgumentException();
        }

        this.bitCount = bitCount;
        data = new BigInteger(1, raw);

        byte[] limitRaw = new byte[raw.length];
        for (int i = 0; i < limitRaw.length; i++) {
            limitRaw[i] = (byte) 0xFF;
        }
        limit = new BigInteger(1, BitUtils.truncateBytes(limitRaw, bitCount));
        limit = limit.add(BigInteger.ONE);
        
        if (data.compareTo(limit) > 0) {
            throw new IllegalArgumentException();
        }
    }

    public BitLimitedId add(BitLimitedId other) {
        if (other == null) {
            throw new NullPointerException();
        }
        if (bitCount != other.bitCount) {
            throw new IllegalArgumentException();
        }

        BigInteger added = data.add(other.data);
        byte[] addedByteArray = BitUtils.truncateBytes(added.toByteArray(),
                bitCount);

        BitLimitedId addedId = new BitLimitedId(bitCount,
                new BigInteger(1, addedByteArray),
                limit);

        return addedId;
    }

    public BitLimitedId subtract(BitLimitedId other) {
        if (other == null) {
            throw new NullPointerException();
        }
        if (bitCount != other.bitCount) {
            throw new IllegalArgumentException();
        }

        BigInteger subtracted = data.subtract(other.data);
        if (subtracted.signum() == -1) {
            subtracted = subtracted.add(limit);
        }
        // just in case. not really required
        byte[] subtractedByteArray = BitUtils.truncateBytes(
                subtracted.toByteArray(), bitCount);

        BitLimitedId subtractedId = new BitLimitedId(bitCount,
                new BigInteger(1, subtractedByteArray),
                limit);

        return subtractedId;
    }

    public int comparePosition(BitLimitedId base, BitLimitedId other) {
        if (base == null || other == null) {
            throw new NullPointerException();
        }
        if (bitCount != base.bitCount || bitCount != other.bitCount) {
            throw new IllegalArgumentException();
        }
        
        BitLimitedId otherOffsetId = other.subtract(base);
        BitLimitedId offsetId = this.subtract(base);

        BigInteger otherOffsetIdNum = otherOffsetId.asBigInteger();
        BigInteger offsetIdNum = offsetId.asBigInteger();
        
        return offsetIdNum.compareTo(otherOffsetIdNum);
    }

    public boolean isWithin(BitLimitedId lower, boolean lowerInclusive,
            BitLimitedId upper, boolean upperInclusive) {
        if (lower == null || upper == null) {
            throw new NullPointerException();
        }
        
        return isWithin(lower, lower, lowerInclusive, upper, upperInclusive);
    }
    
    public boolean isWithin(BitLimitedId base, BitLimitedId lower,
            boolean lowerInclusive, BitLimitedId upper,
            boolean upperInclusive) {
        if (lower == null || upper == null || base == null) {
            throw new NullPointerException();
        }
        
        if (base.bitCount != bitCount || lower.bitCount != bitCount
                || upper.bitCount != bitCount) {
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
    
    public int getBitCount() {
        return bitCount;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.data);
        hash = 89 * hash + this.bitCount;
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
        final BitLimitedId other = (BitLimitedId) obj;
        if (!Objects.equals(this.data, other.data)) {
            return false;
        }
        if (this.bitCount != other.bitCount) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "BitLimitedId{" + "data=" + data + ", limit=" + limit
                + ", bitCount=" + bitCount + '}';
    }

}
