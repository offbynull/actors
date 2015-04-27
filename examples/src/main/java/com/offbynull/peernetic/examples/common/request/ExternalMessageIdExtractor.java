package com.offbynull.peernetic.examples.common.request;

import org.apache.commons.lang3.Validate;

public final class ExternalMessageIdExtractor {

    public String getId(Object msg) {
        Validate.notNull(msg);
        Validate.isTrue(msg instanceof ExternalMessage);
        
        ExternalMessage externalMessage = (ExternalMessage) msg;
        return Long.toHexString(externalMessage.getId());
    }
    
}
