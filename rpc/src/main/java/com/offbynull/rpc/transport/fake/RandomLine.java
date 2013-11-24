package com.offbynull.rpc.transport.fake;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.Validate;

public final class RandomLine<A> implements Line<A> {
    private Random random;
    private double minDropRate;
    private double maxDropRate;
    private double minRepeatRate;
    private double maxRepeatRate;
    private int maxRepeats;
    private int chunkSize;
    private int minAllowedChunks;
    private int maxAllowedChunks;
    private int minDurationPerChunk;
    private int maxDurationPerChunk;
    private int minJitterPerChunk;
    private int maxJitterPerChunk;

    public RandomLine(long seed, double minDropRate, double maxDropRate, double minRepeatRate, double maxRepeatRate, int maxRepeats,
            int chunkSize, int minChunks, int maxChunks, int minDurationPerChunk, int maxDurationPerChunk, int minJitterPerChunk,
            int maxJitterPerChunk) {
        Validate.inclusiveBetween(0.0, 1.0, minDropRate);
        Validate.inclusiveBetween(minDropRate, 1.0, maxDropRate);
        Validate.inclusiveBetween(0.0, 1.0, minRepeatRate);
        Validate.inclusiveBetween(minRepeatRate, 1.0, maxRepeatRate);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, maxRepeats);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, chunkSize);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, minChunks);
        Validate.inclusiveBetween(minChunks, Integer.MAX_VALUE, maxChunks);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, minDurationPerChunk);
        Validate.inclusiveBetween(minDurationPerChunk, Integer.MAX_VALUE, maxDurationPerChunk);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, minJitterPerChunk);
        Validate.inclusiveBetween(minJitterPerChunk, Integer.MAX_VALUE, maxJitterPerChunk);
                
        this.random = new Random(seed);
        this.minDropRate = minDropRate;
        this.maxDropRate = maxDropRate;
        this.minRepeatRate = minRepeatRate;
        this.maxRepeatRate = maxRepeatRate;
        this.maxRepeats = maxRepeats;
        this.chunkSize = chunkSize;
        this.minAllowedChunks = minChunks;
        this.maxAllowedChunks = maxChunks;
        this.minDurationPerChunk = minDurationPerChunk;
        this.maxDurationPerChunk = maxDurationPerChunk;
        this.minJitterPerChunk = minJitterPerChunk;
        this.maxJitterPerChunk = maxJitterPerChunk;
    }
    

    @Override
    public List<Packet<A>> queue(A from, A to, ByteBuffer data) {
        double dropRate = randomDoubleBetween(minDropRate, maxDropRate);
        double repeatRate = randomDoubleBetween(minRepeatRate, maxRepeatRate);
        int allowedChunks = randomIntegerBetween(minAllowedChunks, maxAllowedChunks);
        
        int chunks = (data.remaining() / chunkSize) + (data.remaining() % (chunkSize == 0 ? 0 : 1));
        if (chunks > allowedChunks) {
            return Collections.emptyList();
        }
        
        if (random.nextDouble() <= dropRate) {
            return Collections.emptyList();
        }
        
        List<Packet<A>> packets = new LinkedList<>();
        
        int repeatCount = 0;
        while (random.nextDouble() <= repeatRate && repeatCount < maxRepeats) {
            int durationPerChunk = randomIntegerBetween(minDurationPerChunk, maxDurationPerChunk);
            int jitterPerChunk = randomIntegerBetween(minJitterPerChunk, maxJitterPerChunk);
        
            long arrivalTime = System.currentTimeMillis() + (durationPerChunk * chunks) + (jitterPerChunk * chunks);
            
            packets.add(new Packet<>(from, to, data, arrivalTime));
            repeatCount++;
        }
        
        return packets;
    }
    
    private double randomDoubleBetween(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }

    private int randomIntegerBetween(int min, int max) {
        return min + (max - min) * Math.abs(random.nextInt());
    }
    
    @Override
    public void unqueue(Collection<Packet<A>> packets) {
    }
}
