package com.offbynull.peernetic.core.actors.reliableproxy;

import java.util.Random;
import org.apache.commons.lang3.Validate;

public class StartRetrySendProxy {
    private final String timerPrefix;
    private final String sourcePrefix;
    private final String destinationPrefix;
    private final IdExtractor idExtractor;
    private final SendGuidelineGenerator generator;

    public StartRetrySendProxy(String timerPrefix, String sourcePrefix, String destinationPrefix, IdExtractor idExtractor,
            SendGuidelineGenerator generator) {
        Validate.notNull(timerPrefix);
        Validate.notNull(sourcePrefix);
        Validate.notNull(destinationPrefix);
        Validate.notNull(idExtractor);
        Validate.notNull(generator);
        this.timerPrefix = timerPrefix;
        this.sourcePrefix = sourcePrefix;
        this.destinationPrefix = destinationPrefix;
        this.idExtractor = idExtractor;
        this.generator = generator;
    }

    public String getTimerPrefix() {
        return timerPrefix;
    }

    public String getSourcePrefix() {
        return sourcePrefix;
    }
    
    public String getDestinationPrefix() {
        return destinationPrefix;
    }

    public IdExtractor getIdExtractor() {
        return idExtractor;
    }

    public SendGuidelineGenerator getGenerator() {
        return generator;
    }

    
}
