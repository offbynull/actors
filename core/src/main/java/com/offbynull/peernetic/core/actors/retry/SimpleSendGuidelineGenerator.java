package com.offbynull.peernetic.core.actors.retry;

import java.time.Duration;
import java.util.Collections;
import org.apache.commons.lang3.Validate;

public final class SimpleSendGuidelineGenerator implements SendGuidelineGenerator {

    private static final Duration DEFAULT_WAIT_DURATION = Duration.ofSeconds(2L);
    private static final int DEFAULT_SEND_COUNT = 10;
    
    private final SendGuideline guideline;
    
    public SimpleSendGuidelineGenerator() {
        this(DEFAULT_WAIT_DURATION, DEFAULT_SEND_COUNT);
    }
    
    public SimpleSendGuidelineGenerator(Duration waitDuration, int sendCount) {
        Validate.notNull(waitDuration);
        Validate.isTrue(!waitDuration.isNegative());
        Validate.isTrue(sendCount > 0);
        Duration[] sendDurations = Collections.nCopies(sendCount, waitDuration).toArray(new Duration[sendCount]);
        Duration ackWaitDuration = waitDuration.multipliedBy(sendCount).multipliedBy(2L);
        
        guideline = new SendGuideline(ackWaitDuration, sendDurations);
    }
    
    @Override
    public SendGuideline generate(Object msg) {
        Validate.notNull(msg);
        return guideline;
    }
    
}
