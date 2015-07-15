package com.offbynull.peernetic.examples.raft.internalmessages;

// marker that triggers a follower to become a candidate -- should be added to timer at rand(150,300)
public final class ElectionTimeout {
}
