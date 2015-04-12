package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.peernetic.examples.unstructured.externalmessages.ExternalMessage;
import org.apache.commons.lang3.Validate;

public final class IdExtractor implements com.offbynull.peernetic.core.actors.retry.IdExtractor {

    @Override
    public String getId(Object msg) {
        Validate.notNull(msg);
        Validate.isTrue(msg instanceof ExternalMessage);
        
        ExternalMessage externalMessage = (ExternalMessage) msg;
        return Long.toHexString(externalMessage.getId());
    }
    
}
