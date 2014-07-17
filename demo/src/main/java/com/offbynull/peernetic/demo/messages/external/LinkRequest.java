package com.offbynull.peernetic.demo.messages.external;

import org.apache.commons.lang3.Validate;

public final class LinkRequest extends Request {

    private String key;

    public LinkRequest(String key, String nonce) {
        super(nonce);
        
        Validate.isTrue(key.length() == 32);
        this.key = key;
    }

    public String getKey() {
        return key;
    }

}
