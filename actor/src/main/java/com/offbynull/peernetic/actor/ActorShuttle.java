package com.offbynull.peernetic.actor;

import java.util.Collection;
import org.apache.commons.lang3.Validate;

public final class ActorShuttle implements Shuttle {

    private String prefix;
    private Bus bus;

    ActorShuttle(String prefix, Bus bus) {
        Validate.notNull(prefix);
        Validate.notEmpty(prefix);
        Validate.notNull(bus);

        this.prefix = prefix;
        this.bus = bus;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void send(Collection<Message> messages) {
        messages.forEach(x -> {
            String dst = x.getDestinationAddress();
            String dstPrefix = ActorUtils.getPrefix(dst);
            Validate.isTrue(dstPrefix.equals(prefix));
        });
        bus.add(messages); // throws npe if null or contains null
    }

}
