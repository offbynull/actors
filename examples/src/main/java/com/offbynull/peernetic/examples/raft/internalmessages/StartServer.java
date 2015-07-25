package com.offbynull.peernetic.examples.raft.internalmessages;

import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.shuttle.Address;
import java.util.HashSet;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class StartServer {
    private final AddressTransformer addressTransformer;
    private final UnmodifiableSet<String> nodeLinks;
    private final long seed;
    private final Address timerPrefix;
    private final Address graphAddress;
    private final Address logAddress;
    
    public StartServer(
            AddressTransformer addressTransformer,
            HashSet<String> nodeLinks,
            long seed,
            Address timerPrefix,
            Address graphAddress,
            Address logAddress) {
        Validate.notNull(addressTransformer);
        // bootstrapAddress can be null
        Validate.notNull(timerPrefix);
        Validate.notNull(graphAddress);
        this.addressTransformer = addressTransformer;
        this.nodeLinks = (UnmodifiableSet<String>) UnmodifiableSet.unmodifiableSet(new HashSet<String>(nodeLinks));
        this.seed = seed;
        this.timerPrefix = timerPrefix;
        this.graphAddress = graphAddress;
        this.logAddress = logAddress;
    }

    public AddressTransformer getAddressTransformer() {
        return addressTransformer;
    }

    public UnmodifiableSet<String> getNodeLinks() {
        return nodeLinks;
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
