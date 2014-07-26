package com.offbynull.peernetic.debug.testnetwork;

import java.time.Duration;

public final class SimpleLineFactory implements LineFactory {

    public static final SimpleLineFactory DEFAULT = new SimpleLineFactory();
    
    private long randomSeed;
    private Duration minDelayPerKb;
    private Duration maxJitterPerKb;
    private double dropChancePerKb;
    private double nonRepeatChancePerKb;
    private int maxRepeat;

    public SimpleLineFactory() {
        this(0L, Duration.ZERO, Duration.ZERO, 0.0, 1.0, 0); // no delay, no jitter, no dropped packets, no repeating packets
    }

    public SimpleLineFactory(long randomSeed, Duration minDelayPerKb, Duration maxJitterPerKb, double dropChancePerKb,
            double nonRepeatChancePerKb, int maxRepeat) {
        // Create object just to test args
        new SimpleLine<>(randomSeed, minDelayPerKb, maxJitterPerKb, dropChancePerKb, nonRepeatChancePerKb, maxRepeat);
        
        this.randomSeed = randomSeed;
        this.minDelayPerKb = minDelayPerKb;
        this.maxJitterPerKb = maxJitterPerKb;
        this.dropChancePerKb = dropChancePerKb; // chance * kb = how likely it'll drop
        // so 0.1 * 10kb = 1.0 -- when means it'll get dropped 100% of the time
        this.nonRepeatChancePerKb = nonRepeatChancePerKb; // 1 - (chance * kb) = how likely it WON'T repeat
        // so 1 - (0.1 * 10kb) = 1 - 1.0 = 0 -- which means it'll never repeat once size >= 10kb
        // repeat rate = rate/kb = 0.1rate/10kb = 0.01 repeat count = 0
        // repeat rate = rate/kb = 0.1rate/1kb = 1 repeat count = 1
        this.maxRepeat = maxRepeat;
    }
    
    @Override
    public <A> Line<A> createLine() {
        return new SimpleLine<>(randomSeed, minDelayPerKb, maxJitterPerKb, dropChancePerKb, nonRepeatChancePerKb, maxRepeat);
    }
    
}
