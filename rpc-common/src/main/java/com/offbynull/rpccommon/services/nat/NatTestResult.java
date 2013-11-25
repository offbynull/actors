package com.offbynull.rpccommon.services.nat;

import java.net.InetSocketAddress;
import org.apache.commons.lang3.Validate;

public final class NatTestResult {
    private InetSocketAddress exposedAddress;
    private boolean exposedAddressMatchesLocalAddress;
    private boolean exposedPortMatchesRpcPort;
    private boolean accessibleUdp;
    private boolean accessibleTcp;

    public NatTestResult(InetSocketAddress exposedAddress, boolean exposedAddressMatchesLocalAddress, boolean exposedPortMatchesRpcPort,
            boolean accessibleUdp, boolean accessibleTcp) {
        Validate.notNull(exposedAddress);
        this.exposedAddress = exposedAddress;
        this.exposedAddressMatchesLocalAddress = exposedAddressMatchesLocalAddress;
        this.exposedPortMatchesRpcPort = exposedPortMatchesRpcPort;
        this.accessibleUdp = accessibleUdp;
        this.accessibleTcp = accessibleTcp;
    }

    public InetSocketAddress getExposedAddress() {
        return exposedAddress;
    }

    public boolean isAccessibleUdp() {
        return accessibleUdp;
    }

    public boolean isAccessibleTcp() {
        return accessibleTcp;
    }

    public boolean isExposedAddressMatchesLocalAddress() {
        return exposedAddressMatchesLocalAddress;
    }

    public boolean isExposedPortMatchesRpcPort() {
        return exposedPortMatchesRpcPort; // this is totally worthless for tcp
    }

    @Override
    public String toString() {
        return "NatTestResult{" + "exposedAddress=" + exposedAddress + ", exposedAddressMatchesLocalAddress="
                + exposedAddressMatchesLocalAddress + ", exposedPortMatchesRpcPort=" + exposedPortMatchesRpcPort + ", accessibleUdp="
                + accessibleUdp + ", accessibleTcp=" + accessibleTcp + '}';
    }
    
}
