package com.offbynull.peernetic.debug.testnetwork.messages;

import org.apache.commons.lang3.Validate;

public final class JoinHub<A> {
    private A address;

    public JoinHub(A address) {
        Validate.notNull(address);
        this.address = address;
    }

    public A getAddress() {
        return address;
    }

}
