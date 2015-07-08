package com.offbynull.peernetic.examples.common;

import com.offbynull.peernetic.core.shuttle.Address;

/**
 * Transforms {@link Address}es to identifiers and vice-versa. Use an address transformer when you want your addressing logic to be
 * decoupled from your {@link Actor}'s execution logic.
 * <p>
 * Since implementations of this interface are intended to be used from within actors, implementations must be immutable (no shared
 * resources that require synchronization).
 * @author Kasra Faghihi
 */
public interface AddressTransformer {
    String selfAddressToId(Address address);
    String remoteAddressToId(Address address);
    Address idToRemoteAddress(String id);
}
