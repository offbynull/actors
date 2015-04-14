package com.offbynull.peernetic.examples.common.request;

import java.util.Random;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableInt;

public final class ExternalMessageIdGenerator {
    private final Random random;
    private final MutableInt seqNum;

    public ExternalMessageIdGenerator(Random random) {
        Validate.notNull(random);
        this.random = random;
        this.seqNum = new MutableInt(random.nextInt());
    }
    
    public long generateId() {
        seqNum.increment();
        return ((long) random.nextInt() << 32L) | ((long) seqNum.intValue());
    }
}
