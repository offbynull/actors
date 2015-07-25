package com.offbynull.peernetic.examples.raft.externalmessages;

import java.io.Serializable;
import org.apache.commons.lang3.Validate;

public final class PullEntryResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final Object value;
    private final int index;

    public PullEntryResponse(Object value, int index) {
        Validate.notNull(value);
        Validate.isTrue(index >= 0);
        this.value = value;
        this.index = index;
    }

    public Object getValue() {
        return value;
    }

    public int getIndex() {
        return index;
    }

}
