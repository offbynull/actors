package com.offbynull.peernetic.examples.unstructured.internalmessages;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class Start {

    private final UnmodifiableSet<String> bootstrapAddresses;
    private final Random random;
    private final String timerPrefix;
    private final String graphAddress;

    public Start(Random random, String timerPrefix, String graphAddress) {
        this(Collections.emptySet(), random, timerPrefix, graphAddress);
    }
    
    public Start(String bootstrapAddress, Random random, String timerPrefix, String graphAddress) {
        this(Collections.singleton(bootstrapAddress), random, timerPrefix, graphAddress);
    }
    
    public Start(Set<String> bootstrapAddresses, Random random, String timerPrefix, String graphAddress) {
        Validate.notNull(bootstrapAddresses);
        Validate.notNull(random);
        Validate.noNullElements(bootstrapAddresses);
        this.bootstrapAddresses = (UnmodifiableSet<String>) UnmodifiableSet.unmodifiableSet(new LinkedHashSet<String>(bootstrapAddresses));
        this.random = random;
        this.timerPrefix = timerPrefix;
        this.graphAddress = graphAddress;
    }

    public UnmodifiableSet<String> getBootstrapAddresses() {
        return bootstrapAddresses;
    }

    public Random getRandom() {
        return random;
    }

    public String getTimerPrefix() {
        return timerPrefix;
    }

    public String getGraphAddress() {
        return graphAddress;
    }

}
