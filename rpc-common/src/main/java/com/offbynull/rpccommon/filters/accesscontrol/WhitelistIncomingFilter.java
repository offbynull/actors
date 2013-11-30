package com.offbynull.rpccommon.filters.accesscontrol;

import com.offbynull.rpc.transport.IncomingFilter;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.Validate;

public final class WhitelistIncomingFilter<A> implements IncomingFilter<A> {

    private Set<A> allowedSet;

    public WhitelistIncomingFilter() {
        this(Collections.<A>emptySet());
    }
    
    public WhitelistIncomingFilter(Set<A> disallowedSet) {
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
    public ByteBuffer filter(A from, ByteBuffer buffer) {
        if (!allowedSet.contains(from)) {
            throw new AddressNotInWhitelistException();
        }
        
        return buffer;
    }

    public static class AddressNotInWhitelistException extends RuntimeException {
    }
}
