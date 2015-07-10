package com.offbynull.peernetic.examples.chord;

import com.offbynull.peernetic.core.shuttle.Address;

final class AddressConstants {
    // in case you forget, this is all threadsafe... for more information read
    // http://stackoverflow.com/questions/8865086/why-is-this-static-final-variable-in-a-singleton-thread-safe
    
    private AddressConstants() {
    }
    
    public static final String JOIN_ELEMENT_NAME = "join";
    public static final String ROUTER_ELEMENT_NAME = "router";
    public static final String UPDATEOTHERS_ELEMENT_NAME = "updateothers";
    public static final String FIXFINGER_ELEMENT_NAME = "fixfinger";
    public static final String STABILIZE_ELEMENT_NAME = "stabilize";
    public static final String CHECKPRED_ELEMENT_NAME = "checkpred";
    public static final String HANDLER_ELEMENT_NAME = "handler";

    
    public static final Address JOIN_RELATIVE_ADDRESS = Address.of(JOIN_ELEMENT_NAME);
    public static final Address ROUTER_RELATIVE_ADDRESS = Address.of(ROUTER_ELEMENT_NAME);
    public static final Address ROUTER_UPDATEOTHERS_RELATIVE_ADDRESS = Address.of(ROUTER_ELEMENT_NAME, UPDATEOTHERS_ELEMENT_NAME);
    public static final Address ROUTER_FIXFINGER_RELATIVE_ADDRESS = Address.of(ROUTER_ELEMENT_NAME, FIXFINGER_ELEMENT_NAME);
    public static final Address ROUTER_STABILIZE_RELATIVE_ADDRESS = Address.of(ROUTER_ELEMENT_NAME, STABILIZE_ELEMENT_NAME);
    public static final Address ROUTER_CHECKPRED_RELATIVE_ADDRESS = Address.of(ROUTER_ELEMENT_NAME, CHECKPRED_ELEMENT_NAME);
    public static final Address ROUTER_HANDLER_RELATIVE_ADDRESS = Address.of(ROUTER_ELEMENT_NAME, HANDLER_ELEMENT_NAME);
    
}
