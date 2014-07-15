package com.offbynull.peernetic.demo.messages.external;

import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class FailureResponse extends Response {

    private Set<Object> links;

    public FailureResponse(Set<Object> links, String nonce) {
        super(nonce);
        this.links = links;
        
        validate();
    }

    @Override
    protected void innerValidate() {
        Validate.notNull(links);
    }


}
