package com.offbynull.rpccommon.services;

import com.offbynull.rpc.RpcInvokeKeys;
import com.offbynull.rpc.invoke.InvokeThreadInformation;

public final class AddressExposerServiceImplementation implements AddressExposerService {

    @Override
    public String getCallingAddress() {
        Object obj = InvokeThreadInformation.getInfo(RpcInvokeKeys.FROM_ADDRESS);
        return obj == null ? null : obj.toString();
    }
    
}
