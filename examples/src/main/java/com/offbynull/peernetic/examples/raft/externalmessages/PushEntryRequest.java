package com.offbynull.peernetic.examples.raft.externalmessages;

import java.io.Serializable;
import org.apache.commons.lang3.Validate;

public final class PushEntryRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final Object value;

    public PushEntryRequest(Object value) {
        Validate.notNull(value);
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

}
