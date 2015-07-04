package com.offbynull.peernetic.examples.chord.internalmessages;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.chord.model.NodeId;
import org.apache.commons.lang3.Validate;

public final class Start {

    private final Address bootstrapAddress;
    private final NodeId nodeId;
    private final long seed;
    private final Address timerPrefix;
    private final Address logAddress;
    private final Address graphAddress;

    public Start(NodeId nodeId, long seed, Address timerPrefix, Address graphAddress, Address logAddress) {
        this(null, nodeId, seed, timerPrefix, graphAddress, logAddress);
    }
    
    public Start(Address bootstrapAddress, NodeId nodeId, long seed, Address timerPrefix, Address graphAddress, Address logAddress) {
        // bootstrapAddress can be null
        Validate.notNull(nodeId);
        Validate.notNull(seed);
        Validate.notNull(timerPrefix);
        Validate.notNull(graphAddress);
        Validate.notNull(logAddress);
        
        this.bootstrapAddress = bootstrapAddress;
        this.nodeId = nodeId;
        this.seed = seed;
        this.timerPrefix = timerPrefix;
        this.graphAddress = graphAddress;
        this.logAddress = logAddress;
    }

    public Address getBootstrapAddress() {
        return bootstrapAddress;
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
