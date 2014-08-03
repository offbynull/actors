package com.offbynull.peernetic.demos.unstructured.messages.external;

import com.offbynull.peernetic.common.Response;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;

public final class QueryResponse<A> extends Response {

    private UnmodifiableList<A> links;

    public QueryResponse(List<A> links, byte[] nonce) {
        super(nonce);
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

}
