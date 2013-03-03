package com.offbynull.peernetic.chord.messages.shared;

import java.util.Objects;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public final class NodeAddress {
    private String host;
    private int port;

    public NodeAddress() {
    }

    @NotNull
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
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
        hash = 97 * hash + Objects.hashCode(this.host);
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
        if (!Objects.equals(this.host, other.host)) {
            return false;
        }
        if (this.port != other.port) {
            return false;
        }
        return true;
    }
}
