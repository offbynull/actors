package com.offbynull.peernetic.debug.testnetwork;

import java.time.Duration;
import org.apache.commons.lang3.Validate;

public final class SimpleLineFactory implements LineFactory {

    public static final SimpleLineFactory DEFAULT = new SimpleLineFactory();
    
    private long randomSeed;
    private Duration delayPerKb;
    private Duration jitterPerKb;
    private double dropChancePerKb;
    private double nonRepeatChancePerKb;

    public SimpleLineFactory() {
        this(0L, Duration.ZERO, Duration.ZERO, 0.0, 1.0); // no delay, no jitter, no dropped packets, no repeating packets
    }

    public SimpleLineFactory(long randomSeed, Duration delayPerKb, Duration jitterPerKb, double dropChancePerKb,
            double nonRepeatChancePerKb) {
        Validate.notNull(delayPerKb);
        Validate.notNull(jitterPerKb);
        Validate.notNull(!delayPerKb.isNegative() && !jitterPerKb.isNegative()
                && dropChancePerKb >= 0.0 && nonRepeatChancePerKb >= 0.0
                && dropChancePerKb <= 1.0 && nonRepeatChancePerKb <= 1.0);
        this.randomSeed = randomSeed;
        this.delayPerKb = delayPerKb;
        this.jitterPerKb = jitterPerKb;
        this.dropChancePerKb = dropChancePerKb; // chance * kb = how likely it'll drop
        // so 0.1 * 10kb = 1.0 -- when means it'll get dropped 100% of the time
        this.nonRepeatChancePerKb = nonRepeatChancePerKb; // 1 - (chance * kb) = how likely it WON'T repeat
        // so 1 - (0.1 * 10kb) = 1 - 1.0 = 0 -- which means it'll never repeat once size >= 10kb
        // repeat rate = rate/kb = 0.1rate/10kb = 0.01 repeat count = 0
        // repeat rate = rate/kb = 0.1rate/1kb = 1 repeat count = 1
    }
    
    @Override
    public <A> Line<A> createLine() {
        return new SimpleLine<>(randomSeed, delayPerKb, jitterPerKb, dropChancePerKb, nonRepeatChancePerKb);
    }
    
}
