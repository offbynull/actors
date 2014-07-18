package com.offbynull.peernetic.demo.messages.external;

public final class LinkRequest extends Request {

    public LinkRequest(String key, String nonce) {
        super(nonce);
    }
}
