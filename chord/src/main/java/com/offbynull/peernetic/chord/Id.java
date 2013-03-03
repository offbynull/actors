package com.offbynull.peernetic.chord;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

public final class Id {

    private BigInteger data;
    private BigInteger limit;
    private int bitCount;

    private Id(int bitCount, BigInteger data, BigInteger limit) {
        this.data = data;
        this.limit = limit;
        this.bitCount = bitCount;
    }

    public Id(int bitCount, byte[] raw) {
        if (raw == null) {
            throw new NullPointerException();
        }

        if (bitCount <= 0) {
            throw new IllegalArgumentException();
        }

        this.bitCount = bitCount;
        data = new BigInteger(1, truncateBytes(raw));

        byte[] limitRaw = new byte[raw.length];
        for (int i = 0; i < limitRaw.length; i++) {
            limitRaw[i] = (byte) 0xFF;
        }
        limit = new BigInteger(1, truncateBytes(limitRaw));
        limit = limit.add(BigInteger.ONE);
    }

    public Id add(Id other) {
        if (other == null) {
            throw new NullPointerException();
        }
        if (bitCount != other.bitCount) {
            throw new IllegalArgumentException();
        }

        BigInteger added = data.add(other.data);
        byte[] addedByteArray = truncateBytes(added.toByteArray());

        Id addedId = new Id(bitCount,
                new BigInteger(1, addedByteArray),
                limit);

        return addedId;
    }

    public Id subtract(Id other) {
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
        byte[] subtractedByteArray = truncateBytes(subtracted.toByteArray());

        Id subtractedId = new Id(bitCount,
                new BigInteger(1, subtractedByteArray),
                limit);

        return subtractedId;
    }

    public int comparePosition(Id base, Id other) {
        if (base == null || other == null) {
            throw new NullPointerException();
        }
        if (bitCount != base.bitCount || bitCount != other.bitCount) {
            throw new IllegalArgumentException();
        }
        
        Id otherOffsetId = other.subtract(base);
        Id offsetId = this.subtract(base);

        BigInteger otherOffsetIdNum = otherOffsetId.asBigInteger();
        BigInteger offsetIdNum = offsetId.asBigInteger();
        
        return offsetIdNum.compareTo(otherOffsetIdNum);
    }

    public boolean isWithin(Id lower, boolean lowerInclusive,
            Id upper, boolean upperInclusive) {
        if (lower == null || upper == null) {
            throw new NullPointerException();
        }
        
        return isWithin(lower, lower, lowerInclusive, upper, upperInclusive);
    }
    
    public boolean isWithin(Id base, Id lower, boolean lowerInclusive,
            Id upper, boolean upperInclusive) {
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

    private byte[] truncateBytes(byte[] bytes) {
        if (bytes.length * 8 < bitCount) {
            return Arrays.copyOf(bytes, bytes.length);
        }

        boolean alignedToByte = (bitCount % 8) == 0 ? true : false;
        int finalBytesRemainder = 8 - (bitCount % 8);
        int finalBytesCount = (bitCount / 8)
                + (alignedToByte ? 0 : 1);
        int diffBytes = bytes.length - finalBytesCount;

        byte[] finalBytes = new byte[finalBytesCount];
        System.arraycopy(bytes, diffBytes, finalBytes, 0, finalBytesCount);

        if (!alignedToByte) {
            byte mask = -128; // 1000 0000
            mask >>= finalBytesRemainder - 1;
            mask = (byte) ~mask;
            finalBytes[0] = (byte) (finalBytes[0] & mask);
        }

        return finalBytes;
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
        final Id other = (Id) obj;
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
        return "Id{" + "data=" + data + ", limit=" + limit + ", bitCount=" + bitCount + '}';
    }
    
}
