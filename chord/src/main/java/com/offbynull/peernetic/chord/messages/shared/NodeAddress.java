package com.offbynull.peernetic.chord.messages.shared;

import java.util.Objects;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class NodeAddress {
    private byte[] ip;
    private int port;

    public NodeAddress() {
    }

    @NotNull
    @Size(min = 4, max = 16)
    public byte[] getIp() {
        return ip;
    }

    public void setIp(byte[] ip) {
        this.ip = ip;
    }

    @Min(1)
    @Max(65535)
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.ip);
        hash = 97 * hash + this.port;
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
        final NodeAddress other = (NodeAddress) obj;
        if (!Objects.equals(this.ip, other.ip)) {
            return false;
        }
        if (this.port != other.port) {
            return false;
        }
        return true;
    }
}
