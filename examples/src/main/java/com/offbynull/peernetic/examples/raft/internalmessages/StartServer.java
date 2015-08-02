package com.offbynull.peernetic.examples.raft.internalmessages;

import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.IdGenerator;
import com.offbynull.peernetic.core.shuttle.Address;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class StartServer {
    
    private final AddressTransformer addressTransformer;
    private final int minElectionTimeout;
    private final int maxElectionTimeout;
    private final UnmodifiableSet<String> nodeLinks;
    private final byte[] seed;
    private final Address timerAddress;
    private final Address graphAddress;
    private final Address logAddress;
    
    public StartServer(
            AddressTransformer addressTransformer,
            int minElectionTimeout,
            int maxElectionTimeout,
            Set<String> nodeLinks,
            byte[] seed,
            Address timerAddress,
            Address graphAddress,
            Address logAddress) {
        Validate.notNull(addressTransformer);
        Validate.notNull(nodeLinks);
        Validate.notNull(seed);
        Validate.notNull(timerAddress);
        Validate.notNull(graphAddress);
        Validate.isTrue(seed.length >= IdGenerator.MIN_SEED_SIZE);
        Validate.isTrue(minElectionTimeout >= 0);
        Validate.isTrue(maxElectionTimeout >= 0);
        Validate.isTrue(minElectionTimeout <= maxElectionTimeout);
        this.addressTransformer = addressTransformer;
        this.minElectionTimeout = minElectionTimeout;
        this.maxElectionTimeout = maxElectionTimeout;
        this.nodeLinks = (UnmodifiableSet<String>) UnmodifiableSet.unmodifiableSet(new HashSet<String>(nodeLinks));
        this.seed = Arrays.copyOf(seed, seed.length);
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
