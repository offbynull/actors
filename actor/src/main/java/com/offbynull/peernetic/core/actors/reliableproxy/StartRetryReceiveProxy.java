package com.offbynull.peernetic.core.actors.reliableproxy;

import org.apache.commons.lang3.Validate;

public class StartRetryReceiveProxy {
    private final String timerPrefix;
    private final String destinationAddress;
    private final ReceiveGuidelineGenerator generator;

    public StartRetryReceiveProxy(String timerPrefix, String destinationAddress, ReceiveGuidelineGenerator generator) {
        Validate.notNull(timerPrefix);
        Validate.notNull(destinationAddress);
        Validate.notNull(generator);
        this.timerPrefix = timerPrefix;
        this.destinationAddress = destinationAddress;
        this.generator = generator;
    }

    public String getTimerPrefix() {
        return timerPrefix;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }
    
    public ReceiveGuidelineGenerator getGenerator() {
        return generator;
    }
}
