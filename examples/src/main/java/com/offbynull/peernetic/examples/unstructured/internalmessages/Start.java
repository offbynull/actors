package com.offbynull.peernetic.examples.unstructured.internalmessages;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.IdGenerator;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class Start {

    private final AddressTransformer addressTransformer;
    private final UnmodifiableSet<String> bootstrapLinks;
    private final byte[] seed;
    private final Address timerAddress;
    private final Address graphAddress;
    private final Address logAddress;
    
    public Start(
            AddressTransformer addressTransformer,
            String bootstrapLink,
            byte[] seed,
            Address timerAddress,
            Address graphAddress,
            Address logAddress) {
        Validate.notNull(addressTransformer);
        // bootstrapAddress can be null
        Validate.notNull(seed);
        Validate.notNull(timerAddress);
        Validate.notNull(graphAddress);
        Validate.isTrue(seed.length >= IdGenerator.MIN_SEED_SIZE);
        this.addressTransformer = addressTransformer;
        Set<String> bootstrapLinkSet = new LinkedHashSet<>();
        if (bootstrapLink != null) {
            bootstrapLinkSet.add(bootstrapLink);
        }
        this.bootstrapLinks = (UnmodifiableSet<String>) UnmodifiableSet.unmodifiableSet(bootstrapLinkSet);
        this.seed = Arrays.copyOf(seed, seed.length);
        this.timerAddress = timerAddress;
        this.graphAddress = graphAddress;
        this.logAddress = logAddress;
    }

    public AddressTransformer getAddressTransformer() {
        return addressTransformer;
    }

    public UnmodifiableSet<String> getBootstrapLinks() {
        return bootstrapLinks;
    }

    public byte[] getSeed() {
        return Arrays.copyOf(seed, seed.length);
    }

    public Address getTimerAddress() {
        return timerAddress;
    }

    public Address getGraphAddress() {
        return graphAddress;
    }

    public Address getLogAddress() {
        return logAddress;
    }

}
