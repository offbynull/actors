package com.offbynull.peernetic.p2ptools.identification;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Objects;

// Why? becuase this ensures that things will always remain unresolved (no
// blocking)
public final class Address {
    private InetSocketAddress address;

    public Address(byte[] ip, int port) {
        if (ip == null) {
            throw new NullPointerException();
        }
        
        try {
            address = new InetSocketAddress(InetAddress.getByAddress(ip), port);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public byte[] getIp() {
        return address.getAddress().getAddress();
    }

    public String getIpAsString() {
        return address.getAddress().getHostAddress();
    }
    
    public int getPort() {
        return address.getPort();
    }
    
    public InetSocketAddress toUnresolvedInetSocketAddress() {
        return new InetSocketAddress(address.getAddress(), address.getPort());
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.address);
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
        if (!Objects.equals(this.address, other.address)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Address{" + "address=" + address + '}';
    }

}
