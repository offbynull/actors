package com.offbynull.peernetic.examples.unstructured.externalmessages;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class QueryResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UnmodifiableSet<String> linkIds;

    public QueryResponse(Set<String> linkIds) {
        Validate.notNull(linkIds);
        Validate.noNullElements(linkIds);
        this.linkIds = (UnmodifiableSet<String>) UnmodifiableSet.unmodifiableSet(new HashSet<String>(linkIds));
    }

    public UnmodifiableSet<String> getLinkIds() {
        return linkIds;
    }
}
