package com.offbynull.peernetic.examples.unstructured.internalmessages;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.common.AddressTransformer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class Start {

    private final AddressTransformer addressTransformer;
    private final UnmodifiableSet<String> bootstrapLinks;
    private final long seed;
    private final Address timerPrefix;
    private final Address graphAddress;
    private final Address logAddress;
    
    public Start(
            AddressTransformer addressTransformer,
            String bootstrapLink,
            long seed,
            Address timerPrefix,
            Address graphAddress,
            Address logAddress) {
        Validate.notNull(addressTransformer);
        // bootstrapAddress can be null
        Validate.notNull(timerPrefix);
        Validate.notNull(graphAddress);
        this.addressTransformer = addressTransformer;
        Set<String> bootstrapLinkSet = new LinkedHashSet<>();
        if (bootstrapLink != null) {
            bootstrapLinkSet.add(bootstrapLink);
        }
        this.bootstrapLinks = (UnmodifiableSet<String>) UnmodifiableSet.unmodifiableSet(bootstrapLinkSet);
        this.seed = seed;
        this.timerPrefix = timerPrefix;
        this.graphAddress = graphAddress;
        this.logAddress = logAddress;
    }

    public AddressTransformer getAddressTransformer() {
        return addressTransformer;
    }

    public UnmodifiableSet<String> getBootstrapLinks() {
        return bootstrapLinks;
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

    public Address getLogAddress() {
        return logAddress;
    }

}
