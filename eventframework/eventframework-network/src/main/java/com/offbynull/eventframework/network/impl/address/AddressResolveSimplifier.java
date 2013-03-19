package com.offbynull.eventframework.network.impl.address;

import com.offbynull.peernetic.eventframework.event.OutgoingEvent;
import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.simplifier.OutgoingSimplifier;
import com.offbynull.peernetic.eventframework.simplifier.SimplifierResult;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class AddressResolveSimplifier
        implements OutgoingSimplifier<AddressResolvedIncomingEvent>{

    private static final Set<Class<? extends OutgoingEvent>> HANDLED_EVENTS;
    static {
        Set<Class<? extends OutgoingEvent>> set = new HashSet<>();
        set.add(ResolveAddressOutgoingEvent.class);
        HANDLED_EVENTS = Collections.unmodifiableSet(set);
    }
    
    @Override
    public Set<Class<? extends OutgoingEvent>> viewHandledEvents() {
        return HANDLED_EVENTS;
    }

    @Override
    public SimplifierResult<AddressResolvedIncomingEvent> simplify(
            OutgoingEvent event) {
        if (event instanceof ResolveAddressOutgoingEvent) {
            ResolveAddressOutgoingEvent raoe =
                    (ResolveAddressOutgoingEvent) event;
            
            Processor<AddressResolvedIncomingEvent> proc =
                    new AddressResolveProcessor(raoe.getAddress());
            return new SimplifierResult<>(proc, true);
        }
        
        return null;
    }
}
