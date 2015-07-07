package com.offbynull.peernetic.examples.common;

import com.offbynull.peernetic.core.shuttle.Address;

// implementations must be immutable (immutable only, no threading constructs like locking or atomics)
public interface AddressTransformer {
    String selfAddressToId(Address address);
    String remoteAddressToId(Address address);
    Address idToRemoteAddress(String id);
}
