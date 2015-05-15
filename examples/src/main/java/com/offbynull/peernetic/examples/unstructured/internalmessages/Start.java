package com.offbynull.peernetic.examples.unstructured.internalmessages;

import com.offbynull.peernetic.core.shuttle.Address;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class Start {

    private final UnmodifiableSet<Address> bootstrapAddresses;
    private final Random random;
    private final Address timerPrefix;
    private final Address graphAddress;

    public Start(Random random, Address timerPrefix, Address graphAddress) {
        this(Collections.emptySet(), random, timerPrefix, graphAddress);
    }
    
    public Start(Address bootstrapAddress, Random random, Address timerPrefix, Address graphAddress) {
        this(Collections.singleton(bootstrapAddress), random, timerPrefix, graphAddress);
    }
    
    public Start(Set<Address> bootstrapAddresses, Random random, Address timerPrefix, Address graphAddress) {
        Validate.notNull(bootstrapAddresses);
        Validate.notNull(random);
        Validate.noNullElements(bootstrapAddresses);
        this.bootstrapAddresses = (UnmodifiableSet<Address>) UnmodifiableSet.unmodifiableSet(new LinkedHashSet<Address>(bootstrapAddresses));
        this.random = random;
        this.timerPrefix = timerPrefix;
        this.graphAddress = graphAddress;
    }

    public UnmodifiableSet<Address> getBootstrapAddresses() {
        return bootstrapAddresses;
    }

    public Random getRandom() {
        return random;
    }

    public Address getTimerPrefix() {
        return timerPrefix;
    }

    public Address getGraphAddress() {
        return graphAddress;
    }

}
