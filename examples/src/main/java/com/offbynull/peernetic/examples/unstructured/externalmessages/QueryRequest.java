package com.offbynull.peernetic.examples.unstructured.externalmessages;

import com.offbynull.peernetic.examples.common.request.ExternalMessage;

public final class QueryRequest extends ExternalMessage {

    public QueryRequest(long id) {
        super(id);
    }
}
