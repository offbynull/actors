package com.offbynull.peernetic.playground.unstructuredmesh.messages.external;

import com.offbynull.peernetic.common.message.Response;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;

public final class QueryResponse<A> extends Response {

    private UnmodifiableList<A> links;

    public QueryResponse(List<A> links) {
        this.links = (UnmodifiableList<A>) UnmodifiableList.unmodifiableList(new ArrayList<A>(links));
        innerValidate();
    }

    public UnmodifiableList<A> getLinks() {
        return links;
    }

    @Override
    protected void innerValidate() {
        try {
            Validate.noNullElements(links);
        } catch (RuntimeException re) {
            throw new IllegalStateException();
        }
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
        final QueryResponse other = (QueryResponse) obj;
        return super.equals(obj);
    }
}
