package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.peernetic.core.shuttle.Address;

final class AddressConstants {
    // in case you forget, this is all threadsafe... for more information read
    // http://stackoverflow.com/questions/8865086/why-is-this-static-final-variable-in-a-singleton-thread-safe
    
    private AddressConstants() {
    }
    
    public static final String ROUTER_ELEMENT_NAME = "router";
    public static final String HANDLER_ELEMENT_NAME = "handler";
    public static final String QUERIER_ELEMENT_NAME = "querier";
    public static final String OUT_ELEMENT_NAME_FORMAT = "out%d";
    
    public static final Address ROUTER_RELATIVE_ADDRESS = Address.of(ROUTER_ELEMENT_NAME);
    public static final Address ROUTER_HANDLER_RELATIVE_ADDRESS = Address.of(ROUTER_ELEMENT_NAME, HANDLER_ELEMENT_NAME);
    public static final Address ROUTER_QUERIER_RELATIVE_ADDRESS = Address.of(ROUTER_ELEMENT_NAME, QUERIER_ELEMENT_NAME);
    
}
