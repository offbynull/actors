package com.offbynull.peernetic.unstructuredmesh.externalmessages;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;

public final class QueryResponse extends ExternalMessage {

    private final UnmodifiableList<String> links;

    public QueryResponse(long id, List<String> links) {
        super(id);
        Validate.notNull(links);
        Validate.noNullElements(links);
        this.links = (UnmodifiableList<String>) UnmodifiableList.unmodifiableList(new ArrayList<String>(links));
    }

    public UnmodifiableList<String> getLinks() {
        return links;
    }
}
