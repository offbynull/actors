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
package com.offbynull.peernetic.network.actors.udpsimulator;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple {@link Line} implementation. Messages can have delays and jitter applied. Messages can also be dropped or repeated.
 * <p>
 * This class ignores packet size, meaning that you can't specify an MTU or otherwise provide behaviour based on packet/message size. This
 * class also doesn't provide any way to introduce errors in to packets.
 * @author Kasra Faghihi
 */
public final class SimpleLine implements Line {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleLine.class);
    
    private final Random random;
    private final Duration minDelay;
    private final Duration maxJitter;
    private final double dropChance;
    private final double repeatChance;
    private final int maxSend;

    /**
     * Constructs a {@link SimpleLine} instance. Equivalent to calling
     * {@code new SimpleLine(randomSeed, Duration.ZERO, Duration.ZERO, 0.0, 0.0, 1)}, which sets this line to sent messages immediately and
     * reliably (packets are not dropped or duplicated).
     * @param randomSeed seed to use for calculations
     */
    public SimpleLine(long randomSeed) {
        this(randomSeed, Duration.ZERO, Duration.ZERO, 0.0, 0.0, 1); // no delay, no jitter, no dropped packets, no repeating packets
    }

    /**
     * Constructs a {@link SimpleLine} instance.
     * @param randomSeed seed to use for calculations
     * @param minDelay minimum delay of packets -- all packets will sit around for at least this amount of time
     * @param maxJitter maximum jitter of packets -- amount of time (random between 0 and this value) to add to {@code minDelay} for each
     * packet
     * @param dropChance chance that a packet will get dropped -- 0.0 (0% chance) to 1.0 (100% chance)
     * @param repeatChance chance that a packet will repeat -- 0.0 (0% chance) to 1.0 (100% chance)
     * @param maxSend maximum number of times a packet can be sent -- must be at least 1
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code 0.0 > dropChance > 1.0}, {@code 0.0 > repeatChance > 1.0}, or {@code maxSend < 1}
     */
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
