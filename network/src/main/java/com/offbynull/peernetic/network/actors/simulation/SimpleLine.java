/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.network.actors.simulation;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
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
    public Collection<TransitMessage> processOutgoing(Instant time, DepartMessage departMessage) {
        return process(time, departMessage);
    }

    @Override
    public Collection<TransitMessage> processIncoming(Instant time, DepartMessage departMessage) {
        return process(time, departMessage);
    }
    
    private Collection<TransitMessage> process(Instant time, DepartMessage departMessage) {
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
            
            TransitMessage transitMsg = new TransitMessage(departMessage.getSourceId(), departMessage.getDestinationAddress(),
                    departMessage.getMessage(), time, delay);
            ret.add(transitMsg);
            
            sendCount++;
        } while (sendCount < maxSend && random.nextDouble() < repeatChance);
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Message from {} to {} will be sent {} times: {}",
                    departMessage.getSourceId(),
                    departMessage.getDestinationAddress(),
                    sendCount,
                    departMessage.getMessage());
        }
        
        return ret;
    }
}
