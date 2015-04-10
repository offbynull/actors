package com.offbynull.peernetic.core.gateways.udp;

import java.io.Serializable;
import org.apache.commons.lang3.Validate;

final class EncapsulatedMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String dstSuffix;
    private final Object object;

    public EncapsulatedMessage(String dstSuffix, Object object) {
        Validate.notNull(object);
        
        this.dstSuffix = dstSuffix; // this can be null
        this.object = object; // this technically shouldn't be null
    }

    public String getAddressSuffix() {
        return dstSuffix;
    }

    public Object getObject() {
        // since this is an object that's sent by UDP transport, we should check here as well, because someone could easily serialize this
        // field to be null
        Validate.notNull(object);
        return object;
    }
    
}
