package com.offbynull.peernetic.core.actors.unreliable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleLine implements Line {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleLine.class);
    
    private final Random random;
    private final Duration minDelay;
    private final Duration maxJitter;
    private final double dropChance;
    private final double repeatChance;
    private final int maxSend;

    public SimpleLine(long randomSeed) {
        this(randomSeed, Duration.ZERO, Duration.ZERO, 0.0, 0.0, 1); // no delay, no jitter, no dropped packets, no repeating packets
    }

    public SimpleLine(long randomSeed, Duration minDelay, Duration maxJitter, double dropChance, double repeatChance, int maxSend) {
        Validate.notNull(minDelay);
        Validate.notNull(maxJitter);
        Validate.isTrue(!minDelay.isNegative() && !maxJitter.isNegative()
                && dropChance >= 0.0 && repeatChance >= 0.0
                && dropChance <= 1.0 && repeatChance <= 1.0
                && maxSend >= 1);
        this.random = new Random(randomSeed);
        this.minDelay = minDelay;
        this.maxJitter = maxJitter;
        this.dropChance = dropChance;
        this.repeatChance = repeatChance;
        this.maxSend = maxSend;
    }

    @Override
    public void nodeJoin(String address) {
        Validate.notNull(address);
        LOG.debug("Adding node {}", address);
        // do nothing
    }

    @Override
    public void nodeLeave(String address) {
        Validate.notNull(address);
        LOG.debug("Removing node {}", address);
        // do nothing
    }

    @Override
    public Collection<TransitMessage> messageDepart(Instant time, DepartMessage departMessage) {
        Validate.notNull(time);
        Validate.notNull(departMessage);

        List<TransitMessage> ret = new ArrayList<>();
        int sendCount = 0;
        do {
            if (random.nextDouble() < dropChance) {
                continue;
            }

            
            // calc jitter
            long maxJitterMillis = maxJitter.toMillis();
            long jitter = maxJitterMillis == 0 ? 0L : random.nextLong() % maxJitterMillis;
            
            // delay = calculated jitter + min delay
            Duration delay = minDelay.plusMillis(jitter);
            delay = delay.isNegative() ? Duration.ZERO : delay; // if neg, give back 0
            
            TransitMessage transitMsg = new TransitMessage(departMessage.getSourceSuffix(), departMessage.getDestinationAddress(),
                    departMessage.getMessage(), time, delay);
            ret.add(transitMsg);
            
            sendCount++;
        } while (sendCount < maxSend && random.nextDouble() < repeatChance);
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Message from {} to {} will be sent {} times: {}",
                    departMessage.getSourceSuffix(),
                    departMessage.getDestinationAddress(),
                    sendCount,
                    departMessage.getMessage());
        }
        
        return ret;
    }

    @Override
    public Collection<DepartMessage> messageArrive(Instant time, TransitMessage transitMessage) {
        Validate.notNull(time);
        Validate.notNull(transitMessage);
        
        DepartMessage departMessage = new DepartMessage(transitMessage.getMessage(), transitMessage.getSourceSuffix(),
                transitMessage.getDestinationAddress());
        return Collections.singleton(departMessage);
    }
}
