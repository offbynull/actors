package com.offbynull.rpccommon.services.nat;

import com.offbynull.rpc.invoke.AsyncResultListener;
import com.offbynull.rpccommon.services.nat.NatHelperService.ConnectionType;
import com.offbynull.rpccommon.services.nat.NatHelperService.TestPortResult;

public interface NatHelperServiceAsync {
    void getAddress(AsyncResultListener<String> result);
    void testPort(AsyncResultListener<TestPortResult> result, ConnectionType type, int port, byte[] challenge);
}
