package com.offbynull.peernetic.examples.raft.internalmessages;

import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.shuttle.Address;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class StartClient {
    private final AddressTransformer addressTransformer;
    private final int minElectionTimeout;
    private final int maxElectionTimeout;
    private final UnmodifiableSet<String> nodeLinks;
    private final Address timerAddress;
    private final Address graphAddress;
    private final Address logAddress;
    
    public StartClient(
            AddressTransformer addressTransformer,
            int minElectionTimeout,
            int maxElectionTimeout,
            Set<String> nodeLinks,
            Address timerAddress,
            Address graphAddress,
            Address logAddress) {
        Validate.notNull(addressTransformer);
        // bootstrapAddress can be null
        Validate.notNull(timerAddress);
        Validate.notNull(graphAddress);
        Validate.isTrue(minElectionTimeout >= 0);
        Validate.isTrue(maxElectionTimeout >= 0);
        Validate.isTrue(minElectionTimeout <= maxElectionTimeout);
        this.addressTransformer = addressTransformer;
        this.minElectionTimeout = minElectionTimeout;
        this.maxElectionTimeout = maxElectionTimeout;
        this.nodeLinks = (UnmodifiableSet<String>) UnmodifiableSet.unmodifiableSet(new HashSet<String>(nodeLinks));
        this.timerAddress = timerAddress;
        this.graphAddress = graphAddress;
        this.logAddress = logAddress;
    }

    public AddressTransformer getAddressTransformer() {
        return addressTransformer;
    }

    public int getMinElectionTimeout() {
        return minElectionTimeout;
    }

    public int getMaxElectionTimeout() {
        return maxElectionTimeout;
    }

    public UnmodifiableSet<String> getNodeLinks() {
        return nodeLinks;
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
