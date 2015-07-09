package com.offbynull.peernetic.examples.chord.internalmessages;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.chord.model.NodeId;
import com.offbynull.peernetic.examples.common.AddressTransformer;
import org.apache.commons.lang3.Validate;

public final class Start {

    private final AddressTransformer addressTransformer;
    private final String bootstrapLinkId;
    private final NodeId nodeId;
    private final long seed;
    private final Address timerPrefix;
    private final Address logAddress;
    private final Address graphAddress;
    
    public Start(
            AddressTransformer addressTransformer,
            String bootstrapLinkId,
            NodeId nodeId,
            long seed,
            Address timerPrefix,
            Address graphAddress,
            Address logAddress) {
        Validate.notNull(addressTransformer);
        // bootstrapAddress can be null
        Validate.notNull(nodeId);
        Validate.notNull(seed);
        Validate.notNull(timerPrefix);
        Validate.notNull(graphAddress);
        Validate.notNull(logAddress);
        
        this.addressTransformer = addressTransformer;
        this.bootstrapLinkId = bootstrapLinkId;
        this.nodeId = nodeId;
        this.seed = seed;
        this.timerPrefix = timerPrefix;
        this.graphAddress = graphAddress;
        this.logAddress = logAddress;
    }

    public AddressTransformer getAddressTransformer() {
        return addressTransformer;
    }

    public String getBootstrapLinkId() {
        return bootstrapLinkId;
    }

    public NodeId getNodeId() {
        return nodeId;
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
