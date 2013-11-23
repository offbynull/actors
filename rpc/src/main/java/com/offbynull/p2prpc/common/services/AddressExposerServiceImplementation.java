package com.offbynull.p2prpc.common.services;

import com.offbynull.p2prpc.RpcInvokeKeys;
import com.offbynull.p2prpc.invoke.InvokeThreadInformation;

public final class AddressExposerServiceImplementation implements AddressExposerService {

    @Override
    public String getCallingAddress() {
        Object obj = InvokeThreadInformation.getInfo(RpcInvokeKeys.FROM_ADDRESS);
        return obj == null ? null : obj.toString();
    }
    
}
