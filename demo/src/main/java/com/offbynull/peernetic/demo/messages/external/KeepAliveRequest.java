package com.offbynull.peernetic.demo.messages.external;

import org.apache.commons.lang3.Validate;

public final class KeepAliveRequest extends Request {

    private String key;

    public KeepAliveRequest(String key, String nonce) {
        super(nonce);
        
        Validate.isTrue(key.length() == 32);
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    
}
