package com.offbynull.peernetic.demos.unstructured.messages.external;

import com.offbynull.peernetic.common.Request;

public final class LinkRequest extends Request {

    public LinkRequest() {
        super(new byte[1]); // fake nonce which should be replaced later
    }

    @Override
    protected void innerValidate() {
        // Does nothing
    }
}
