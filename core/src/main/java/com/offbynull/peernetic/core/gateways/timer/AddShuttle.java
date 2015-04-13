package com.offbynull.peernetic.core.gateways.timer;

import com.offbynull.peernetic.core.shuttle.Shuttle;
import org.apache.commons.lang3.Validate;

final class AddShuttle {
    private final Shuttle shuttle;

    public AddShuttle(Shuttle shuttle) {
        Validate.notNull(shuttle);
        this.shuttle = shuttle;
    }

    public Shuttle getShuttle() {
        return shuttle;
    }
    
}
