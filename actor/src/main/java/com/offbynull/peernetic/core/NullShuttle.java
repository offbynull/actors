package com.offbynull.peernetic.core;

import java.util.Collection;
import org.apache.commons.lang3.Validate;

public final class NullShuttle implements Shuttle {

    private final String prefix;

    public NullShuttle(String prefix) {
        Validate.notNull(prefix);
        this.prefix = prefix;
    }
    
    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void send(Collection<Message> messages) {
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        // do nothing
    }
    
}
