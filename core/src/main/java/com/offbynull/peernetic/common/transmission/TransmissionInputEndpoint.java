package com.offbynull.peernetic.common.transmission;

import com.offbynull.peernetic.actor.Endpoint;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

public final class TransmissionInputEndpoint<A> implements Endpoint {

    private Endpoint transActorEndpoint;
    private A address;

    public TransmissionInputEndpoint(Endpoint transActorEndpoint, A address) {
        Validate.notNull(transActorEndpoint);
        Validate.notNull(address);

        this.transActorEndpoint = transActorEndpoint;
        this.address = address;
    }

    public A getAddress() {
        return address;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.transActorEndpoint);
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
        final TransmissionInputEndpoint other = (TransmissionInputEndpoint) obj;
        if (!Objects.equals(this.transActorEndpoint, other.transActorEndpoint)) {
            return false;
        }
        if (!Objects.equals(this.address, other.address)) {
            return false;
        }
        return true;
    }

    @Override
    public void send(Endpoint source, Object message) {
        Validate.notNull(source);
        Validate.notNull(message);

        transActorEndpoint.send(source, new OutgoingMessageEvent<>(message, address));
    }
}
