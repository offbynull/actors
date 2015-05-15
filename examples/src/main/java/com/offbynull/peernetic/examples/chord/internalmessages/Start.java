package com.offbynull.peernetic.examples.chord.internalmessages;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import java.util.Random;
import org.apache.commons.lang3.Validate;

public final class Start {

    private final Address bootstrapAddress;
    private final NodeId nodeId;
    private final Random random;
    private final Address timerPrefix;
    private final Address graphAddress;

    public Start(NodeId nodeId, Random random, Address timerPrefix, Address graphAddress) {
        this(null, nodeId, random, timerPrefix, graphAddress);
    }
    
    public Start(Address bootstrapAddress, NodeId nodeId, Random random, Address timerPrefix, Address graphAddress) {
        // bootstrapAddress can be null
        Validate.notNull(nodeId);
        Validate.notNull(random);
        Validate.notNull(timerPrefix);
        Validate.notNull(graphAddress);
        
        this.bootstrapAddress = bootstrapAddress;
        this.nodeId = nodeId;
        this.random = random;
        this.timerPrefix = timerPrefix;
        this.graphAddress = graphAddress;
    }

    public Address getBootstrapAddress() {
        return bootstrapAddress;
    }

    public NodeId getNodeId() {
        return nodeId;
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
