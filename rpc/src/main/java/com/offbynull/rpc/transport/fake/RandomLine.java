package com.offbynull.rpc.transport.fake;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.Validate;

/**
 * A line who's characteristics are determined by property ranges. A random number gets picked between a range and gets multiplied by the
 * number of bytes to get that property. Properties include: drop rate, repeat rate, duration, jitter. So, for example, if a
 * {@link RandomLine} object with a drop rate of 0.01 gets asked to process a 1000 byte request, there's a 10% chance of that the message
 * won't make it out.
 * @author Kasra F
 * @param <A> address type
 */
public final class RandomLine<A> implements Line<A> {
    private Random random;
    private Range<Double> dropRatePerByteRange;
    private Range<Double> repeatRatePerByteRange;
    private Range<Double> durationPerByteRange;
    private Range<Double> jitterPerByteRange;

    /**
     * Constructs a {@link RandomLine} object.
     * @param seed seed for random number generator
     * @param dropRatePerByteRange drop rate per byte range 
     * @param repeatRatePerByteRange repeat rate  per byte range
     * @param durationPerByteRange duration per byte range
     * @param jitterPerByteRange jitter per byte range
     */
    public RandomLine(long seed, Range<Double> dropRatePerByteRange, Range<Double> repeatRatePerByteRange,
            Range<Double> durationPerByteRange, Range<Double> jitterPerByteRange) {
        Validate.notNull(dropRatePerByteRange);
        Validate.notNull(repeatRatePerByteRange);
        Validate.notNull(durationPerByteRange);
        Validate.notNull(jitterPerByteRange);
        
        this.dropRatePerByteRange = dropRatePerByteRange;
        this.repeatRatePerByteRange = repeatRatePerByteRange;
        this.durationPerByteRange = durationPerByteRange;
        this.jitterPerByteRange = jitterPerByteRange;
        this.random = new Random(seed);

    }
    

    @Override
    public List<Message<A>> queue(A from, A to, ByteBuffer data) {
        int len = data.remaining();
        
        double repeatRate = randomDoubleBetween(repeatRatePerByteRange) * len;
        
        List<Message<A>> packets = new LinkedList<>();
        
        do {
            double dropRate = randomDoubleBetween(dropRatePerByteRange) * len;
            
            if (random.nextDouble() <= dropRate) {
                continue;
            }
        
            double duration = randomDoubleBetween(durationPerByteRange) * len;
            double jitter = randomDoubleBetween(jitterPerByteRange) * len;
            
            long arrivalTime = System.currentTimeMillis() + (long) duration + (long) jitter;
            
            packets.add(new Message<>(from, to, data, arrivalTime));
        } while (random.nextDouble() <= repeatRate);
        
        return packets;
    }
    
    private double randomDoubleBetween(Range<Double> range) {
        double min = range.getMinimum();
        double max = range.getMaximum();
        return min + (max - min) * random.nextDouble();
    }

    private int randomIntegerBetween(Range<Integer> range) {
        int min = range.getMinimum();
        int max = range.getMaximum();
        return min + (max - min) * Math.abs(random.nextInt());
    }
    
    @Override
    public void unqueue(Collection<Message<A>> packets) {
    }
}
