package com.offbynull.peernetic.examples.chord.internalmessages;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.chord.model.NodeId;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import java.util.Arrays;
import org.apache.commons.lang3.Validate;

public final class Start {

    private final AddressTransformer addressTransformer;
    private final String bootstrapLinkId;
    private final NodeId nodeId;
    private final byte[] seed;
    private final Address timerAddress;
    private final Address logAddress;
    private final Address graphAddress;
    
    public Start(
            AddressTransformer addressTransformer,
            String bootstrapLinkId,
            NodeId nodeId,
            byte[] seed,
            Address timerAddress,
            Address graphAddress,
            Address logAddress) {
        Validate.notNull(addressTransformer);
        // bootstrapAddress can be null
        Validate.notNull(nodeId);
        Validate.notNull(seed);
        Validate.notNull(timerAddress);
        Validate.notNull(graphAddress);
        Validate.notNull(logAddress);
        
        this.addressTransformer = addressTransformer;
        this.bootstrapLinkId = bootstrapLinkId;
        this.nodeId = nodeId;
        this.seed = Arrays.copyOf(seed, seed.length);
        this.timerAddress = timerAddress;
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
