package com.offbynull.peernetic.demo.messages.external;

import org.apache.commons.lang3.Validate;

public final class JoinRequest extends Request {

    private String key;

    public JoinRequest(String key, String nonce) {
        super(nonce);
        
        Validate.isTrue(key.length() == 32);
        this.key = key;
    }

    public String getKey() {
        return key;
    }

}
