package com.offbynull.peernetic.examples.unstructured.externalmessages;

import com.offbynull.peernetic.core.shuttle.Address;
import java.io.Serializable;
import org.apache.commons.lang3.Validate;

public final class LinkSuccessResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final Address address;

    public LinkSuccessResponse(Address address) {
        Validate.notNull(address);
        this.address = address;
    }

    public Address getAddress() {
        return address;
    }
    
}
