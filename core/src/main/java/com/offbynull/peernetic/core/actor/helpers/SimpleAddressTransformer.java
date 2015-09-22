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

import com.offbynull.peernetic.core.shuttle.Address;
import org.apache.commons.lang3.Validate;

/**
 * An {@link AddressTransformer} that takes the last element out of a remote address and uses it as the link identifier (and vice-versa), as
 * well as handles conversion of the local address.
 * <p>
 * This class relies on 3 properties:
 * <ul>
 * <li>remoteAddressBase - prefix of remote addresses</li>
 * <li>selfAddress - address of owning actor</li>
 * <li>selfLinkId - link id of owning actor</li>
 * </ul>
 * How {@link #toLinkId(com.offbynull.peernetic.core.shuttle.Address) } works...
 * <p>
 * When using {@link #toLinkId(com.offbynull.peernetic.core.shuttle.Address) } with a remote address (an address with the prefix
 * {@code remoteAddressBase}, it will return the next address element after the prefix. Note that this means any additional suffix won't
 * be retained when the conversion takes place. For example, if {@code remoteAddressBase=actor_system_0:nodes}, and the address being
 * converted is {@code actor_system_0:nodes:node_1:sub1:msg1}, the link id will be {@code node_1}.
 * <p>
 * When using {@link #toLinkId(com.offbynull.peernetic.core.shuttle.Address) } with your local address (an address with the prefix
 * {@code selfAddress}, it will return {@code selfId}. Note that just like with remote addresses, any additional suffix won't  be retained
 * when the conversion takes place.
 * <p>
 * <p>
 * How {@link #toAddress(java.lang.String) } works...
 * <p>
 * When using {@link #toAddress(java.lang.String) }, if the link id is {@code selfLinkId} then {@code selfAddress} will be returned.
 * Otherwise, the returned address will be the link id appended on to {@code remoteAddressBase}.
 * <p>
 * This class is immutable.
 * @author Kasra Faghihi
 */
public final class SimpleAddressTransformer implements AddressTransformer {

    private final Address remoteBaseAddress;
    private final Address selfAddress;
    private final String selfLinkId;

    /**
     * Constructs a {@link SimpleAddressTransformer} without support for self link id / self address.
     * @param remoteBaseAddress prefix for remote addresses
     * @throws NullPointerException if any argument is {@code null}
     */
    public SimpleAddressTransformer(Address remoteBaseAddress) {
        Validate.notNull(remoteBaseAddress);
        this.remoteBaseAddress = remoteBaseAddress;
        this.selfAddress = null;
        this.selfLinkId = null;
    }

    /**
     * Constructs a {@link SimpleAddressTransformer}.
     * @param remoteBaseAddress prefix for remote addresses
     * @param selfAddress address to return when {@link #toAddress(java.lang.String)} is called with {@code selfLinkId}
     * @param selfLinkId link id to return when {@link #toLinkId(com.offbynull.peernetic.core.shuttle.Address) } is called with an address
     * that starts with {@code selfAddress}
     * @throws NullPointerException if any argument is {@code null}
     */
    public SimpleAddressTransformer(Address remoteBaseAddress, Address selfAddress, String selfLinkId) {
        Validate.notNull(remoteBaseAddress);
        Validate.notNull(selfAddress);
        Validate.notNull(selfLinkId);
        this.remoteBaseAddress = remoteBaseAddress;
        this.selfAddress = selfAddress;
        this.selfLinkId = selfLinkId;
    }

    @Override
    public String toLinkId(Address address) {
        Validate.notNull(address);
        if (selfAddress != null && selfAddress.isPrefixOf(address)) {
            Validate.validState(selfLinkId != null); // sanity check
            return selfLinkId;
        } else if (remoteBaseAddress.isPrefixOf(address)) {
            Validate.isTrue(address.size() > remoteBaseAddress.size()); // must be atleast 1 element larger
            return address.getElement(remoteBaseAddress.size());
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public Address toAddress(String linkId) {
        Validate.notNull(selfLinkId);
        if (selfLinkId != null && selfLinkId.equals(linkId)) {
            Validate.validState(selfAddress != null); // sanity check
            return selfAddress;
        } else {
            return remoteBaseAddress.appendSuffix(linkId);
        }
    }
    
}
