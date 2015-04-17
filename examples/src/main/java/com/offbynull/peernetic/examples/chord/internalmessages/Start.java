package com.offbynull.peernetic.examples.chord.internalmessages;

import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import java.util.Random;
import org.apache.commons.lang3.Validate;

public final class Start {

    private final String bootstrapAddress;
    private final NodeId nodeId;
    private final Random random;
    private final String timerPrefix;
    private final String graphAddress;

    public Start(NodeId nodeId, Random random, String timerPrefix, String graphAddress) {
        this(null, nodeId, random, timerPrefix, graphAddress);
    }
    
    public Start(String bootstrapAddress, NodeId nodeId, Random random, String timerPrefix, String graphAddress) {
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

    public String getBootstrapAddress() {
        return bootstrapAddress;
    }

    public NodeId getNodeId() {
        return nodeId;
    }

    public Random getRandom() {
        return random;
    }

    public String getTimerPrefix() {
        return timerPrefix;
    }

    public String getGraphAddress() {
        return graphAddress;
    }

}
