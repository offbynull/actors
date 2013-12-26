package com.offbynull.peernetic.overlay.unstructured;

import com.offbynull.peernetic.rpc.RpcInvokeKeys;
import com.offbynull.peernetic.rpc.invoke.InvokeThreadInformation;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

public final class UnstructuredServiceImplementation<A> implements UnstructuredService<A> {
    private LinkManager<A> linkManager;

    public UnstructuredServiceImplementation(LinkManager<A> linkManager) {
        Validate.notNull(linkManager);
        this.linkManager = linkManager;
    }

    @Override
    public State<A> getState() {
        return linkManager.getState();
    }

    @Override
    public boolean join(byte[] secret) {
        Validate.isTrue(SECRET_SIZE == secret.length);
        
        A from = InvokeThreadInformation.getInfo(RpcInvokeKeys.FROM_ADDRESS);
        linkManager.addIncomingLink(System.currentTimeMillis(), from, ByteBuffer.wrap(secret));
        
        return true;
    }

    @Override
    public boolean keepAlive(byte[] secret) {
        Validate.isTrue(SECRET_SIZE == secret.length);
        
        A from = InvokeThreadInformation.getInfo(RpcInvokeKeys.FROM_ADDRESS);
        linkManager.updateIncomingLink(System.currentTimeMillis(), from, ByteBuffer.wrap(secret));
        
        return true;
    }
}
