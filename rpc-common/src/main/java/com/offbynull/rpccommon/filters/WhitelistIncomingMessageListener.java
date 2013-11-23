package com.offbynull.rpccommon.filters;

import com.offbynull.rpc.transport.IncomingMessage;
import com.offbynull.rpc.transport.IncomingMessageListener;
import com.offbynull.rpc.transport.IncomingMessageResponseHandler;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.Validate;

public final class WhitelistIncomingMessageListener<A> implements IncomingMessageListener<A> {

    private Set<A> allowedSet;

    public WhitelistIncomingMessageListener(Set<A> disallowedSet) {
        Validate.noNullElements(disallowedSet);

        this.allowedSet = Collections.newSetFromMap(new ConcurrentHashMap<A, Boolean>());
        this.allowedSet.addAll(disallowedSet);
    }

    public void addAddress(A e) {
        allowedSet.add(e);
    }

    public void removeAddress(A e) {
        allowedSet.remove(e);
    }

    public void addAddresses(Collection<? extends A> c) {
        allowedSet.addAll(c);
    }

    public void removeAddresses(Collection<? extends A> c) {
        allowedSet.removeAll(c);
    }

    public void clear() {
        allowedSet.clear();
    }

    @Override
    public void messageArrived(IncomingMessage<A> message, IncomingMessageResponseHandler responseCallback) {
        A from = message.getFrom();

        if (!allowedSet.contains(from)) {
            throw new AddressNotInWhitelistException();
        }
    }

    public static class AddressNotInWhitelistException extends RuntimeException {
    }
}
