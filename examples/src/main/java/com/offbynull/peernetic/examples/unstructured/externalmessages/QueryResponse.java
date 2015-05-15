package com.offbynull.peernetic.examples.unstructured.externalmessages;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.common.request.ExternalMessage;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;

public final class QueryResponse extends ExternalMessage {

    private final UnmodifiableList<Address> links;

    public QueryResponse(long id, List<Address> links) {
        super(id);
        Validate.notNull(links);
        Validate.noNullElements(links);
        this.links = (UnmodifiableList<Address>) UnmodifiableList.unmodifiableList(new ArrayList<Address>(links));
    }

    public UnmodifiableList<Address> getLinks() {
        return links;
    }
}
