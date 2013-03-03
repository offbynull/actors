package com.offbynull.peernetic.chord.messages.shared;

import java.util.Arrays;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public final class NodeId {
    private int bitCount;
    private byte[] data;

    public NodeId() {
    }

    @Min(1)
    public int getBitCount() {
        return bitCount;
    }

    public void setBitCount(int bitCount) {
        this.bitCount = bitCount;
    }

    @NotNull
    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + this.bitCount;
        hash = 41 * hash + Arrays.hashCode(this.data);
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
        final NodeId other = (NodeId) obj;
        if (this.bitCount != other.bitCount) {
            return false;
        }
        if (!Arrays.equals(this.data, other.data)) {
            return false;
        }
        return true;
    }
}
