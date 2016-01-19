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
package com.offbynull.peernetic.io.actors.udpsimulator;

import com.offbynull.peernetic.core.common.Serializer;
import java.nio.ByteBuffer;
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
 * This implementation doesn't provide any way to introduce errors in to packets.
 *
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
    private final int mtu;
    private final Serializer serializer;

    /**
     * Constructs a {@link SimpleLine} instance. Equivalent to calling
     * {@code new SimpleLine(randomSeed, Duration.ZERO, Duration.ZERO, 0.0, 0.0, 1, Integer.MAX_VALUE, serializer)}, which sets this line to
     * sent messages immediately and reliably (packets are not dropped or duplicated).
     *
     * @param randomSeed seed to use for calculations
     * @param serializer serializer to use to serialize messages to packets / deserialize messages from packets
     * @throws NullPointerException if any argument is {@code null}
     */
    public SimpleLine(long randomSeed, Serializer serializer) {
        // no delay, no jitter, no dropped packets, no repeating packets, Integer.MAX_VALUE mtu
        this(randomSeed, Duration.ZERO, Duration.ZERO, 0.0, 0.0, 1, Integer.MAX_VALUE, serializer);
    }

    /**
     * Constructs a {@link SimpleLine} instance.
     *
     * @param randomSeed seed to use for calculations
     * @param minDelay minimum delay of packets -- all packets will sit around for at least this amount of time
     * @param maxJitter maximum jitter of packets -- amount of time (random between 0 and this value) to add to {@code minDelay} for each
     * packet
     * @param dropChance chance that a packet will get dropped -- 0.0 (0% chance) to 1.0 (100% chance)
     * @param repeatChance chance that a packet will repeat -- 0.0 (0% chance) to 1.0 (100% chance)
     * @param maxSend maximum number of times a packet can be sent -- must be at least 1
     * @param mtu maximum tranmission unit for packets
     * @param serializer serializer to use to serialize messages to packets / deserialize messages from packets
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code 0.0 > dropChance > 1.0}, {@code 0.0 > repeatChance > 1.0}, {@code maxSend < 1}, or
     * {@code mtu >= 0}
     */
    public SimpleLine(long randomSeed, Duration minDelay, Duration maxJitter, double dropChance, double repeatChance, int maxSend, int mtu,
            Serializer serializer) {
        Validate.notNull(minDelay);
        Validate.notNull(maxJitter);
        Validate.isTrue(!minDelay.isNegative() && !maxJitter.isNegative()
                && dropChance >= 0.0 && repeatChance >= 0.0
                && dropChance <= 1.0 && repeatChance <= 1.0
                && maxSend >= 1
                && mtu >= 0);
        this.random = new Random(randomSeed);
        this.minDelay = minDelay;
        this.maxJitter = maxJitter;
        this.dropChance = dropChance;
        this.repeatChance = repeatChance;
        this.maxSend = maxSend;
        this.mtu = mtu;
        this.serializer = serializer;
    }

    @Override
    public Collection<TransitMessage> processOutgoing(Instant time, DepartMessage departMessage) {
        return process(time, departMessage, Mode.OUTGOING);
    }

    @Override
    public Collection<TransitMessage> processIncoming(Instant time, DepartMessage departMessage) {
        return process(time, departMessage, Mode.INCOMING);
    }

    private Collection<TransitMessage> process(Instant time, DepartMessage departMessage, Mode mode) {
        Validate.notNull(time);
        Validate.notNull(departMessage);

        // Apply timing/dropping/repeating logic
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

            Object msg = constructMessage(departMessage.getMessage(), mode);
            
            if (msg == null) {
                // Message was not constructed, because the size of the serialized packet is > than the MTU. As such, just break out of the
                // loop. The next iteration will just try to serialize it again (if it makes it this far -- drop logic above) and encounter
                // the same MTU limit
                break;
            }
            TransitMessage transitMsg = new TransitMessage(departMessage.getSourceAddress(), departMessage.getDestinationAddress(),
                    msg, time, delay);
            ret.add(transitMsg);

            sendCount++;
        } while (sendCount < maxSend && random.nextDouble() < repeatChance);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Message from {} to {} will be sent {} times: {}",
                    departMessage.getSourceAddress(),
                    departMessage.getDestinationAddress(),
                    sendCount,
                    departMessage.getMessage());
        }

        return ret;
    }

    private Object constructMessage(Object msg, Mode mode) {
        // Crate msg by serializing or deserializing class
        Object ret;
        ByteBuffer buffer;
        byte[] bufferData;

        switch (mode) {
            case INCOMING: // deserialize on incoming packet
                buffer = ((ByteBuffer) msg).asReadOnlyBuffer();
                bufferData = new byte[buffer.remaining()];
                buffer.get(bufferData);

                ret = serializer.deserialize(bufferData); // ise on error, which is defined in process* javadocs in interface
                break;
            case OUTGOING: // serialize on outgoing packet
                Object obj = msg;
                bufferData = serializer.serialize(obj); // ise on error, which is defined in process* javadocs in interface
                buffer = ByteBuffer.wrap(bufferData);

                ret = bufferData.length > mtu ? null : buffer; // Serialized packet if greater than MTU, so
                break;
            default:
                throw new IllegalStateException(); // should never happen
            }

        return ret;
    }

    private enum Mode {

        OUTGOING,
        INCOMING
    }
}
