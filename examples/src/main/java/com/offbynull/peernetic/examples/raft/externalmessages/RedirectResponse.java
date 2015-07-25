package com.offbynull.peernetic.examples.raft.externalmessages;

import java.io.Serializable;
import org.apache.commons.lang3.Validate;

// node is follower and is telling you to redirect somewhere else
public final class RedirectResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String leaderLinkId;

    public RedirectResponse(String leaderLinkId) {
        Validate.notNull(leaderLinkId);
        this.leaderLinkId = leaderLinkId;
    }

    public String getLeaderLinkId() {
        return leaderLinkId;
    }
    
}
