package com.offbynull.peernetic.core.gateways.timer;

import org.apache.commons.lang3.Validate;

final class RemoveShuttle {
    private final String prefix;

    public RemoveShuttle(String prefix) {
        Validate.notNull(prefix);
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

}
