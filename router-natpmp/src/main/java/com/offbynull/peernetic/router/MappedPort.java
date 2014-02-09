package com.offbynull.peernetic.router;

import java.net.InetAddress;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

public final class MappedPort {
    private int internalPort;
    private int externalPort;
    private InetAddress externalAddress;
    private PortType portType;

    public MappedPort(int internalPort, int externalPort, InetAddress externalAddress, PortType portType) {
        Validate.inclusiveBetween(1, 65535, internalPort);
        Validate.inclusiveBetween(1, 65535, externalPort);
        Validate.notNull(externalAddress);
        Validate.notNull(portType);
        
        this.internalPort = internalPort;
        this.externalPort = externalPort;
        this.externalAddress = externalAddress;
        this.portType = portType;
    }

    public int getInternalPort() {
        return internalPort;
    }

    public int getExternalPort() {
        return externalPort;
    }

    public InetAddress getExternalAddress() {
        return externalAddress;
    }

    public PortType getPortType() {
        return portType;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + this.internalPort;
        hash = 31 * hash + this.externalPort;
        hash = 31 * hash + Objects.hashCode(this.externalAddress);
        hash = 31 * hash + Objects.hashCode(this.portType);
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
        final MappedPort other = (MappedPort) obj;
        if (this.internalPort != other.internalPort) {
            return false;
        }
        if (this.externalPort != other.externalPort) {
            return false;
        }
        if (!Objects.equals(this.externalAddress, other.externalAddress)) {
            return false;
        }
        if (this.portType != other.portType) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MappedPort{" + "internalPort=" + internalPort + ", externalPort=" + externalPort + ", externalAddress=" + externalAddress
                + ", portType=" + portType + '}';
    }
    
}
