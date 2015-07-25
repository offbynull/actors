package com.offbynull.peernetic.examples.raft.externalmessages;

import java.io.Serializable;

// node is in the middle of being a candidate, or is a follower just being set up (hasn't voted for anyone) -- retry later.
public final class RetryResponse implements Serializable {
    private static final long serialVersionUID = 1L;

}
