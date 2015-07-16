package com.offbynull.peernetic.examples.raft;

import com.offbynull.peernetic.core.shuttle.Address;

final class AddressConstants {
    // in case you forget, this is all threadsafe... for more information read
    // http://stackoverflow.com/questions/8865086/why-is-this-static-final-variable-in-a-singleton-thread-safe
    
    private AddressConstants() {
    }
    
    public static final String ROUTER_ELEMENT_NAME = "router";
    public static final String FOLLOWER_ELEMENT_NAME = "follower";
    public static final String CANDIDATE_ELEMENT_NAME = "candidate";
    public static final String LEADER_ELEMENT_NAME = "leader";
    public static final String HANDLER_ELEMENT_NAME = "handler";

    
    public static final Address ROUTER_RELATIVE_ADDRESS = Address.of(ROUTER_ELEMENT_NAME);
    public static final Address ROUTER_FOLLOWER_RELATIVE_ADDRESS = Address.of(ROUTER_ELEMENT_NAME, FOLLOWER_ELEMENT_NAME);
    public static final Address ROUTER_CANDIDATE_RELATIVE_ADDRESS = Address.of(ROUTER_ELEMENT_NAME, CANDIDATE_ELEMENT_NAME);
    public static final Address ROUTER_LEADER_RELATIVE_ADDRESS = Address.of(ROUTER_ELEMENT_NAME, LEADER_ELEMENT_NAME);
    public static final Address ROUTER_HANDLER_RELATIVE_ADDRESS = Address.of(ROUTER_ELEMENT_NAME, HANDLER_ELEMENT_NAME);
    
}
