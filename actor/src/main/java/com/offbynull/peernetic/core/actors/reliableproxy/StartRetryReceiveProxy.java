package com.offbynull.peernetic.core.actors.reliableproxy;

import org.apache.commons.lang3.Validate;

public class StartRetryReceiveProxy {
    private final String timerPrefix;
    private final String destinationAddress;
    private final IdExtractor idExtractor;
    private final ReceiveGuidelineGenerator generator;

    public StartRetryReceiveProxy(String timerPrefix, String destinationAddress, IdExtractor idExtractor,
            ReceiveGuidelineGenerator generator) {
        Validate.notNull(timerPrefix);
        Validate.notNull(destinationAddress);
        Validate.notNull(idExtractor);
        Validate.notNull(generator);
        this.timerPrefix = timerPrefix;
        this.destinationAddress = destinationAddress;
        this.idExtractor = idExtractor;
        this.generator = generator;
    }

    public String getTimerPrefix() {
        return timerPrefix;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public IdExtractor getIdExtractor() {
        return idExtractor;
    }
    
    public ReceiveGuidelineGenerator getGenerator() {
        return generator;
    }
}
