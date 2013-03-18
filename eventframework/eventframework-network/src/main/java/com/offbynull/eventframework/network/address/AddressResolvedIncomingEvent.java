package com.offbynull.eventframework.network.address;

import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class AddressResolvedIncomingEvent implements IncomingEvent {
    private Set<ResolvedAddress> resolvedAddresses;

    public AddressResolvedIncomingEvent(
            Set<ResolvedAddress> resolvedAddresses) {
        if (resolvedAddresses == null) {
            throw new NullPointerException();
        }
        
        this.resolvedAddresses = Collections.unmodifiableSet(
                new LinkedHashSet<>(resolvedAddresses));
    }

    public Set<ResolvedAddress> getResolvedAddresses() {
        return resolvedAddresses;
    }
    
}
