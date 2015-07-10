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
 * An {@link AddressTransformer} that takes the last element out of a remote address and uses it as the id (and vice-versa).
 * <p>
 * Examples of {@link #selfAddressToLinkId(com.offbynull.peernetic.core.shuttle.Address) }...
 * <pre>
 * e.g. remoteBase=actor, selfId=0                       actor:0 -> 0
 * e.g. remoteBase=udp0, selfId=7f000001.10000           actor:0 -> 7f000001.10000
 * e.g. remoteBase=actor:unrel0:actor, selfId=unrel0     actor:0 -> unrel0
 * </pre>
 * <p>
 * Examples of {@link #remoteAddressToLinkId(com.offbynull.peernetic.core.shuttle.Address)  }...
 * <pre>
 * e.g. remoteBase=actor, selfId=0                       actor:0 -> 0
 * e.g. remoteBase=udp0, selfId=7f000001.10000           udp0:7f000001.10001 -> 7f000001.10001
 * e.g. remoteBase=actor:unrel0:actor, selfId=unrel0     actor:unrel0:actor:unrel1 -> unrel1
 * </pre>
 * <p>
 * Examples of {@link #linkIdToRemoteAddress(java.lang.String) }...
 * <pre>
 * e.g. remoteBase=actor, selfId=0                       1 -> actor:1
 * e.g. remoteBase=udp0, selfId=7f000001.10000           7f000001.10001 -> udp0:7f000001.10001
 * e.g. remoteBase=actor:unrel0:actor, selfId=unrel0     unrel1 -> actor:unrel0:actor:unrel1
 * </pre>
 * @author Kasra Faghihi
 */
public final class SimpleAddressTransformer implements AddressTransformer {

    private final Address remoteBase;
    private final String selfId;

    /**
     * Constructs a {@link SimpleAddressTransformer}.
     * @param remoteBase prefix for remote addresses
     * @param selfId identifier to return when {@link #selfAddressToLinkId(com.offbynull.peernetic.core.shuttle.Address) } is called
     * @throws NullPointerException if any argument is {@code null}
     */
    public SimpleAddressTransformer(Address remoteBase, String selfId) {
        Validate.notNull(remoteBase);
        Validate.notNull(selfId);
        this.remoteBase = remoteBase;
        this.selfId = selfId;
    }
    
    // e.g. remoteBase=actor, selfId=0                       actor:0 -> 0
    // e.g. remoteBase=udp0, selfId=7f000001.10000           actor:0 -> 7f000001.10000
    // e.g. remoteBase=actor:unrel0:actor, selfId=unrel0     actor:0 -> unrel0
    @Override
    public String selfAddressToLinkId(Address address) {
        Validate.notNull(address);
        return selfId;
    }

    // e.g. remoteBase=actor, selfId=0                       actor:0 -> 0
    // e.g. remoteBase=udp0, selfId=7f000001.10000           udp0:7f000001.10001 -> 7f000001.10001
    // e.g. remoteBase=actor:unrel0:actor, selfId=unrel0     actor:unrel0:actor:unrel1 -> unrel1
    @Override
    public String remoteAddressToLinkId(Address address) {
        Validate.notNull(address);
        Validate.isTrue(remoteBase.isPrefixOf(address));
        Validate.isTrue(address.size() == remoteBase.size() + 1);
        return address.getElement(remoteBase.size());
    }

    // e.g. remoteBase=actor, selfId=0                       0 -> actor:0
    // e.g. remoteBase=udp0, selfId=7f000001.10000           7f000001.10001 -> udp0:7f000001.10001
    // e.g. remoteBase=actor:unrel0:actor, selfId=unrel0     unrel1 -> actor:unrel0:actor:unrel1
    @Override
    public Address linkIdToRemoteAddress(String id) {
        Validate.notNull(id);
        return remoteBase.appendSuffix(id);
    }
    
}
