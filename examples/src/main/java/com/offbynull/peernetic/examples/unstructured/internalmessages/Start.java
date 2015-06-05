package com.offbynull.peernetic.examples.unstructured.internalmessages;

import com.offbynull.peernetic.core.shuttle.Address;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.Validate;

public final class Start {

    private final Function<Address, String> selfAddressToIdMapper;
    private final Function<Address, String> remoteAddressToIdMapper;
    private final Function<String, Address> idToRemoteAddressMapper;
    private final UnmodifiableSet<Address> bootstrapAddresses;
    private final long seed;
    private final Address timerPrefix;
    private final Address graphAddress;
    private final Address logAddress;

    public Start(
            Function<Address, String> selfAddressToIdMapper,
            Function<Address, String> remoteAddressToIdMapper,
            Function<String, Address> idToRemoteAddressMapper,
            long seed,
            Address timerPrefix,
            Address graphAddress,
            Address logAddress) {
        this(selfAddressToIdMapper,
                remoteAddressToIdMapper,
                idToRemoteAddressMapper,
                Collections.emptySet(),
                seed,
                timerPrefix,
                graphAddress,
                logAddress);
    }
    
    public Start(
            Function<Address, String> selfAddressToIdMapper,
            Function<Address, String> remoteAddressToIdMapper,
            Function<String, Address> idToRemoteAddressMapper,
            Address bootstrapAddress,
            long seed,
            Address timerPrefix,
            Address graphAddress,
            Address logAddress) {
        this(selfAddressToIdMapper,
                remoteAddressToIdMapper,
                idToRemoteAddressMapper,
                Collections.singleton(bootstrapAddress),
                seed,
                timerPrefix,
                graphAddress,
                logAddress);
    }
    
    public Start(
            Function<Address, String> selfAddressToIdMapper,
            Function<Address, String> remoteAddressToIdMapper,
            Function<String, Address> idToRemoteAddressMapper,
            Set<Address> bootstrapAddresses,
            long seed,
            Address timerPrefix,
            Address graphAddress,
            Address logAddress) {
        Validate.notNull(selfAddressToIdMapper);
        Validate.notNull(remoteAddressToIdMapper);
        Validate.notNull(idToRemoteAddressMapper);
        Validate.notNull(bootstrapAddresses);
        Validate.notNull(timerPrefix);
        Validate.notNull(graphAddress);
        Validate.noNullElements(bootstrapAddresses);
        this.selfAddressToIdMapper = selfAddressToIdMapper;
        this.remoteAddressToIdMapper = remoteAddressToIdMapper;
        this.idToRemoteAddressMapper = idToRemoteAddressMapper;
        this.bootstrapAddresses = (UnmodifiableSet<Address>) UnmodifiableSet.unmodifiableSet(new LinkedHashSet<Address>(bootstrapAddresses));
        this.seed = seed;
        this.timerPrefix = timerPrefix;
        this.graphAddress = graphAddress;
        this.logAddress = logAddress;
    }

    public Function<Address, String> getSelfAddressToIdMapper() {
        return selfAddressToIdMapper;
    }

    public Function<Address, String> getRemoteAddressToIdMapper() {
        return remoteAddressToIdMapper;
    }

    public Function<String, Address> getIdToRemoteAddressMapper() {
        return idToRemoteAddressMapper;
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

    public Address getLogAddress() {
        return logAddress;
    }

}
