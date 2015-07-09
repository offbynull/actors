package com.offbynull.peernetic.examples.common;

import com.offbynull.peernetic.core.shuttle.Address;
import org.apache.commons.lang3.Validate;

/**
 * An {@link AddressTransformer} that takes the last element out of a remote address and uses it as the id (and vice-versa).
 * @author Kasra Faghihi
 */
public final class SimpleAddressTransformer implements AddressTransformer {

    private final Address remoteBase;
    private final String selfId;

    public SimpleAddressTransformer(Address remoteBase, String selfId) {
        Validate.notNull(remoteBase);
        Validate.notNull(selfId);
        Validate.notEmpty(selfId);
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
