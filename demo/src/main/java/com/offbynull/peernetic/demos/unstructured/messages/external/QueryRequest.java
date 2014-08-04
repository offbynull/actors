package com.offbynull.peernetic.demos.unstructured.messages.external;

import com.offbynull.peernetic.common.Request;

public final class QueryRequest extends Request {

    public QueryRequest() {
        super(new byte[1]); // insert with fake nonce that gets replaced during sending
    }

    @Override
    protected void innerValidate() {
        // Does nothing
    }
}
