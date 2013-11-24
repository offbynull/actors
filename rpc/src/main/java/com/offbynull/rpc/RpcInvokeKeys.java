package com.offbynull.rpc;

import com.offbynull.rpc.invoke.InvokeThreadInformation;

/**
 * Special keys set in {@link InvokeThreadInformation} for each RPC invokation.
 * @author Kasra F
 */
public enum RpcInvokeKeys {
    /**
     * The source address who triggered the RPC call.
     */
    FROM_ADDRESS,
    /**
     * The {@link Rpc} object.
     */
    RPC
    
}
