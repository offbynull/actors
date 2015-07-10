package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.peernetic.core.shuttle.Address;

final class AddressConstants {
    // in case you forget, this is all threadsafe... for more information read
    // http://stackoverflow.com/questions/8865086/why-is-this-static-final-variable-in-a-singleton-thread-safe
    
    private AddressConstants() {
    }
    
    public static final String ROUTER_ADDRESS_ELEMENT_NAME = "router";
    public static final String HANDLER_ADDRESS_ELEMENT_NAME = "handler";
    
    public static final Address ROUTER_ADDRESS_SUFFIX = Address.of(ROUTER_ADDRESS_ELEMENT_NAME);
    public static final Address HANDLER_ADDRESS_SUFFIX = Address.of(ROUTER_ADDRESS_ELEMENT_NAME, HANDLER_ADDRESS_ELEMENT_NAME);
    
}
