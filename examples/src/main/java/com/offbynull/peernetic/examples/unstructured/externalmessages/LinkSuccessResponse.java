package com.offbynull.peernetic.examples.unstructured.externalmessages;

import com.offbynull.peernetic.core.shuttle.Address;
import java.io.Serializable;
import org.apache.commons.lang3.Validate;

public final class LinkSuccessResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final Address id;

    public LinkSuccessResponse(Address id) {
        Validate.notNull(id);
        this.id = id;
    }

    public Address getId() {
        return id;
    }
    
}
