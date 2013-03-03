package com.offbynull.peernetic.chord;

import java.net.InetSocketAddress;
import java.util.Objects;

// Why not InetSocketAddress? because this is threadsafe nad wont try to resolve
// the hostname to an ip.
public final class Address {
    private String host;
    private int port;

    public Address(String host, int port) {
        if (host == null) {
            throw new NullPointerException();
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException();
        }
        
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
    
    public InetSocketAddress toUnresolvedInetSocketAddress() {
        return InetSocketAddress.createUnresolved(host, port);
    }

    public InetSocketAddress toResolvedInetSocketAddress() {
        InetSocketAddress ret = new InetSocketAddress(host, port);
        if (ret.isUnresolved()) {
            throw new IllegalStateException();
        }
        return ret;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.host);
        hash = 37 * hash + this.port;
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
        final Address other = (Address) obj;
        if (!Objects.equals(this.host, other.host)) {
            return false;
        }
        if (this.port != other.port) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Address{" + "host=" + host + ", port=" + port + '}';
    }
    
}
