/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.peernetic.core.actor.Actor;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.Address;

/**
 * Transforms {@link Address}es to link identifiers and vice-versa. Use an address transformer when you want your addressing logic to be
 * decoupled from your {@link Actor}'s execution logic. This helps simplify communication logic between actors by hiding details that have
 * to do with addressing. For example, if you want your actor to send messages through a proxy actor, you can use a specific address
 * transformer that converts identifiers to addresses that pass through that proxy and vice-versa.
 * <p>
 * Implementations of this interface must be immutable.
 * @author Kasra Faghihi
 */
public interface AddressTransformer {
    /**
     * Converts a self address (e.g. address returned by {@link Context#getSelf()}) to an link identifier.
     * @param address self address
     * @return link identifier for address
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code address} is not of the expected format or is otherwise invalid
     */
    String selfAddressToLinkId(Address address);
    /**
     * Converts a remote address (address that isn't derived from {@link Context#getSelf()}) to a link identifier.
     * @param address remote address
     * @return link identifier for address
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code address} is not of the expected format or is otherwise invalid
     */
    String remoteAddressToLinkId(Address address);
    /**
     * Converts a link identifier to a remote address (address that isn't derived from {@link Context#getSelf()}).
     * @param linkId link identifier
     * @return address for link identifier
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code linkId} is not of the expected format or is otherwise invalid
     */
    Address linkIdToRemoteAddress(String linkId);
}
