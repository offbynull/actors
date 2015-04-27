package com.offbynull.peernetic.core.gateways.recorder;

import java.io.Serializable;
import org.apache.commons.lang3.Validate;

// public because other tools may want to read out recorded data
public final class RecordedMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String srcAddress;
    private String dstSuffix;
    private Object message;

    public RecordedMessage(String srcAddress, String dstSuffix, Object message) {
        Validate.notNull(srcAddress);
        Validate.notNull(dstSuffix);
        Validate.notNull(message);
        this.srcAddress = srcAddress;
        this.dstSuffix = dstSuffix;
        this.message = message;
    }
    
    public String getSrcAddress() {
        return srcAddress;
    }

    public String getDstSuffix() {
        return dstSuffix;
    }

    public Object getMessage() {
        return message;
    }
    
}
