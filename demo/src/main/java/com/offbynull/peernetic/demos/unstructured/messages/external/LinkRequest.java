package com.offbynull.peernetic.demos.unstructured.messages.external;

import com.offbynull.peernetic.common.Request;

public final class LinkRequest extends Request {

    public LinkRequest(byte[] nonce) {
        super(nonce);
    }

    @Override
    protected void innerValidate() {
        // Does nothing
    }
}
