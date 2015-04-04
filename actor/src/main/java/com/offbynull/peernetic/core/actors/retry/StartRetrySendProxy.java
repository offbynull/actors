package com.offbynull.peernetic.core.actors.retry;

import org.apache.commons.lang3.Validate;

public class StartRetrySendProxy {
    private final String timerPrefix;
    private final String destinationAddress;
    private final IdExtractor idExtractor;
    private final SendGuidelineGenerator generator;

    public StartRetrySendProxy(String timerPrefix, String destinationAddress, IdExtractor idExtractor, SendGuidelineGenerator generator) {
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

    public SendGuidelineGenerator getGenerator() {
        return generator;
    }

    
}
