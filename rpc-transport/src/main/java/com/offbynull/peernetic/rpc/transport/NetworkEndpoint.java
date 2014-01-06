package com.offbynull.peernetic.rpc.transport;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.Outgoing;
import com.offbynull.peernetic.rpc.transport.internal.SendMessageCommand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

public final class NetworkEndpoint<A> implements Endpoint {
    private Endpoint transportEndpoint;
    private A address;

    NetworkEndpoint(Endpoint transportEndpoint, A address) {
        Validate.notNull(transportEndpoint);
        Validate.notNull(address);
        
        this.transportEndpoint = transportEndpoint;
        this.address = address;
    }

    @Override
    public void push(Endpoint source, Collection<Outgoing> outgoing) {
        Validate.notNull(source);
        Validate.noNullElements(outgoing);
        
        Collection<Outgoing> transportOutgoing = new ArrayList<>(outgoing.size());
        
        for (Outgoing outgoingMsg : outgoing) {
            Object originalContent = outgoingMsg.getContent();
            
            Object transportContent = new SendMessageCommand<>(originalContent, address);
            Outgoing transportOutgoingMsg = new Outgoing(transportContent, transportEndpoint);
            transportOutgoing.add(transportOutgoingMsg);
        }
        
        transportEndpoint.push(source, transportOutgoing);
    }

    @Override
    public void push(Endpoint source, Outgoing... outgoing) {
        push(source, Arrays.asList(outgoing));
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.transportEndpoint);
        hash = 89 * hash + Objects.hashCode(this.address);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NetworkEndpoint<?> other = (NetworkEndpoint<?>) obj;
        if (!Objects.equals(this.transportEndpoint, other.transportEndpoint)) {
            return false;
        }
        if (!Objects.equals(this.address, other.address)) {
            return false;
        }
        return true;
    }
    
}
