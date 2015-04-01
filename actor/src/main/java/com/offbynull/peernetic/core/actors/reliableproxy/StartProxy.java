package com.offbynull.peernetic.core.actors.reliableproxy;

import java.util.Random;
import org.apache.commons.lang3.Validate;

public class StartProxy {
    private final String timerPrefix;
    private final String destinationAddress;
    private final Random random;
    private final TransmissionGuidelineGenerator generator;

    public StartProxy(String timerPrefix, String destinationAddress, Random random, TransmissionGuidelineGenerator generator) {
        Validate.notNull(timerPrefix);
        Validate.notNull(destinationAddress);
        Validate.notNull(random);
        Validate.notNull(generator);
        this.timerPrefix = timerPrefix;
        this.destinationAddress = destinationAddress;
        this.random = random;
        this.generator = generator;
    }

    public String getTimerPrefix() {
        return timerPrefix;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public Random getRandom() {
        return random;
    }

    public TransmissionGuidelineGenerator getGenerator() {
        return generator;
    }

    
}
