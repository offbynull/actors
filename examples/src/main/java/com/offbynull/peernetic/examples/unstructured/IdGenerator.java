package com.offbynull.peernetic.examples.unstructured;

import java.util.Random;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableInt;

final class IdGenerator {
    private final Random random;
    private final MutableInt seqNum;

    public IdGenerator(Random random) {
        Validate.notNull(random);
        this.random = random;
        this.seqNum = new MutableInt(random.nextInt());
    }
    
    public long generateId() {
        seqNum.increment();
        return ((long) random.nextInt() << 32L) | ((long) seqNum.intValue());
    }
}
