package com.offbynull.peernetic;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.network.Gateway;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

public final class GatewayOutputEndpoint<A> implements Endpoint {

    private Gateway<A> gateway;
    private A address;

    public GatewayOutputEndpoint(Gateway<A> gateway, A address) {
        Validate.notNull(gateway);
        Validate.notNull(address);

        this.gateway = gateway;
        this.address = address;
    }

    public A getAddress() {
        return address;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.gateway);
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
        final GatewayOutputEndpoint other = (GatewayOutputEndpoint) obj;
        if (!Objects.equals(this.gateway, other.gateway)) {
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

        gateway.send(address, message);
    }
}
