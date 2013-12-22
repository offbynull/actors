package com.offbynull.peernetic.overlay.unstructured;

import com.offbynull.peernetic.rpc.RpcInvokeKeys;
import com.offbynull.peernetic.rpc.invoke.InvokeThreadInformation;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class OverlayServiceImplementation<A> implements OverlayService<A> {
    private IncomingLinkMaintainer<A> incomingLinkMaintainer;
    private OutgoingLinkMaintainer<A> outgoingLinkMaintainer;

    public OverlayServiceImplementation(IncomingLinkMaintainer<A> incomingLinkMaintainer,
            OutgoingLinkMaintainer<A> outgoingLinkMaintainer) {
        Validate.notNull(incomingLinkMaintainer);
        Validate.notNull(outgoingLinkMaintainer);

        this.incomingLinkMaintainer = incomingLinkMaintainer;
        this.outgoingLinkMaintainer = outgoingLinkMaintainer;
    }

    @Override
    public Information<A> getInformation() {
        boolean incomingSlotsAvailable = incomingLinkMaintainer.isRoomAvailable();
        // these are copies
        Set<A> incomingLinks = incomingLinkMaintainer.getLinks();
        Set<A> outgoingLinks = outgoingLinkMaintainer.getLinks();
        
        return new Information<>(incomingLinks, outgoingLinks, incomingSlotsAvailable);
    }

    @Override
    public boolean join(byte[] secret) {
        Validate.inclusiveBetween(SECRET_SIZE, SECRET_SIZE, secret.length);

        A from = InvokeThreadInformation.getInfo(RpcInvokeKeys.FROM_ADDRESS);
        
        return incomingLinkMaintainer.createLink(from, ByteBuffer.wrap(Arrays.copyOf(secret, secret.length)));
    }

    @Override
    public void unjoin(byte[] secret) {
        Validate.inclusiveBetween(SECRET_SIZE, SECRET_SIZE, secret.length);
        
        A from = InvokeThreadInformation.getInfo(RpcInvokeKeys.FROM_ADDRESS);
        
        incomingLinkMaintainer.destroyLink(from, ByteBuffer.wrap(Arrays.copyOf(secret, secret.length)));
    }

    @Override
    public boolean keepAlive(byte[] secret) {
        Validate.inclusiveBetween(SECRET_SIZE, SECRET_SIZE, secret.length);
        
        A from = InvokeThreadInformation.getInfo(RpcInvokeKeys.FROM_ADDRESS);
        
        return incomingLinkMaintainer.updateLink(from, ByteBuffer.wrap(Arrays.copyOf(secret, secret.length)));
    }
}
