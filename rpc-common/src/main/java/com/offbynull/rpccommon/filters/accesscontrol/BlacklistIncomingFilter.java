package com.offbynull.rpccommon.filters.accesscontrol;

import com.offbynull.rpc.transport.IncomingFilter;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.Validate;

public final class BlacklistIncomingFilter<A> implements IncomingFilter<A> {
    private Set<A> disallowedSet;

    public BlacklistIncomingFilter() {
        this(Collections.<A>emptySet());
    }
    
    public BlacklistIncomingFilter(Set<A> disallowedSet) {
        Validate.noNullElements(disallowedSet);
        
        this.disallowedSet = Collections.newSetFromMap(new ConcurrentHashMap<A, Boolean>());
        this.disallowedSet.addAll(disallowedSet);
    }

    public void addAddress(A e) {
        disallowedSet.add(e);
    }

    public void removeAddress(A e) {
        disallowedSet.remove(e);
    }

    public void addAddresses(Collection<? extends A> c) {
        disallowedSet.addAll(c);
    }

    public void removeAddresses(Collection<? extends A> c) {
        disallowedSet.removeAll(c);
    }

    public void clear() {
        disallowedSet.clear();
    }
    
    @Override
    public ByteBuffer filter(A from, ByteBuffer buffer) {
        if (disallowedSet.contains(from)) {
            throw new AddressInBlacklistException();
        }
        
        return buffer;
    }
    
    public static class AddressInBlacklistException extends RuntimeException {
    }
}
