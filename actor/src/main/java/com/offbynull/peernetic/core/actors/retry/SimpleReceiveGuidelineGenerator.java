package com.offbynull.peernetic.core.actors.retry;

import java.time.Duration;
import org.apache.commons.lang3.Validate;

public final class SimpleReceiveGuidelineGenerator implements ReceiveGuidelineGenerator {
    private static final Duration DEFAULT_WAIT_DURATION = Duration.ofSeconds(2L).multipliedBy(10L);
    
    private final ReceiveGuideline guideline;

    public SimpleReceiveGuidelineGenerator() {
        this(DEFAULT_WAIT_DURATION);
    }
    
    public SimpleReceiveGuidelineGenerator(Duration waitDuration) {
        Validate.notNull(waitDuration);
        Validate.isTrue(!waitDuration.isNegative());
        
        guideline = new ReceiveGuideline(waitDuration);
    }
    
    @Override
    public ReceiveGuideline generate(Object msg) {
        Validate.notNull(msg);
        return guideline;
    }
    
}
