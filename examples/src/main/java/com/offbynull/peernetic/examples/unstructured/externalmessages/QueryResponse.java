package com.offbynull.peernetic.examples.unstructured.externalmessages;

import com.offbynull.peernetic.core.shuttle.Address;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class QueryResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UnmodifiableSet<Address> links;

    public QueryResponse(Set<Address> links) {
        Validate.notNull(links);
        Validate.noNullElements(links);
        this.links = (UnmodifiableSet<Address>) UnmodifiableSet.unmodifiableSet(new HashSet<Address>(links));
    }

    public UnmodifiableSet<Address> getLinks() {
        return links;
    }
}
