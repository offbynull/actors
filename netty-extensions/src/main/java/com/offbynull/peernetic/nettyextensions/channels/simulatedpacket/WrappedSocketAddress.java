package com.offbynull.peernetic.nettyextensions.channels.simulatedpacket;

import java.net.SocketAddress;
import java.util.Objects;

public final class WrappedSocketAddress extends SocketAddress {
    private Object obj;

    public WrappedSocketAddress(Object obj) {
        this.obj = obj;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.obj);
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
        final WrappedSocketAddress other = (WrappedSocketAddress) obj;
        if (!Objects.equals(this.obj, other.obj)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "WrappedSocketAddress{" + "obj=" + obj + '}';
    }
    
}
