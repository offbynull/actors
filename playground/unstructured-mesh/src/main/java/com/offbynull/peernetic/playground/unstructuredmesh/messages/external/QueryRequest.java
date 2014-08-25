package com.offbynull.peernetic.playground.unstructuredmesh.messages.external;

import com.offbynull.peernetic.common.message.Request;

public final class QueryRequest extends Request {

    public QueryRequest() {
    }

    @Override
    protected void innerValidate() {
        // Does nothing
    }
    
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final QueryRequest other = (QueryRequest) obj;
        return super.equals(obj);
    }
}
