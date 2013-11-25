package com.offbynull.rpccommon.services.nat;

public interface NatHelperService {
    public static final int SERVICE_ID = 2000;
    
    String getAddress();
    TestPortResult testPort(ConnectionType type, int port, byte[] challenge);
    
    public enum ConnectionType {
        TCP,
        UDP
    }
    
    public enum TestPortResult {
        SUCCESS,
        FAIL,
        ERROR
    }
}
