package com.offbynull.peernetic.examples.unstructured.internalmessages;

import com.offbynull.peernetic.core.shuttle.Address;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class Start {

    private final UnmodifiableSet<Address> bootstrapAddresses;
    private final long seed;
    private final Address timerPrefix;
    private final Address graphAddress;

    public Start(long seed, Address timerPrefix, Address graphAddress) {
        this(Collections.emptySet(), seed, timerPrefix, graphAddress);
    }
    
    public Start(Address bootstrapAddress, long seed, Address timerPrefix, Address graphAddress) {
        this(Collections.singleton(bootstrapAddress), seed, timerPrefix, graphAddress);
    }
    
    public Start(Set<Address> bootstrapAddresses, long seed, Address timerPrefix, Address graphAddress) {
        Validate.notNull(bootstrapAddresses);
        Validate.notNull(timerPrefix);
        Validate.notNull(graphAddress);
        Validate.noNullElements(bootstrapAddresses);
        this.bootstrapAddresses = (UnmodifiableSet<Address>) UnmodifiableSet.unmodifiableSet(new LinkedHashSet<Address>(bootstrapAddresses));
        this.seed = seed;
        this.timerPrefix = timerPrefix;
        this.graphAddress = graphAddress;
    }

    public UnmodifiableSet<Address> getBootstrapAddresses() {
        return bootstrapAddresses;
    }

    public long getSeed() {
        return seed;
    }

    public Address getTimerPrefix() {
        return timerPrefix;
    }

    public Address getGraphAddress() {
        return graphAddress;
    }

}
