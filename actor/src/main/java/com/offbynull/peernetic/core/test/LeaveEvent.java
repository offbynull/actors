package com.offbynull.peernetic.core.test;

import java.time.Instant;
import org.apache.commons.lang3.Validate;

final class LeaveEvent extends Event {
    final String address;

    public LeaveEvent(String address, Instant when) {
        super(when);
        Validate.notNull(address);
        this.address = address;
    }

    public String getAddress() {
        return address;
    }
    
}
