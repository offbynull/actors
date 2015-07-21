package com.offbynull.peernetic.examples.raft;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.Controller;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.Validate;

final class State {
    
    private final Random random;
    private int counter = 0;
    
    private final Address timerAddress;
    private final Address graphAddress;
    private final Address logAddress;
    
    private final String selfLinkId;
    private final UnmodifiableSet<String> otherNodeLinkIds; // does not include self
    private final AddressTransformer addressTransformer;
    private final Controller routerController;
    
    // persistent state (not really persistent in this example, but whatever)
    private int currentTerm;
    private String votedForLinkId;
    private List<LogEntry> log;
    
    // volatile state on all servers
    private int commitIndex;
    private int lastApplied;
    
    // volatile state on leaders (reinitialized after election)
    private Map<String, Integer> nextIndex; // key = node link id, value = idx of next log entry to send to that node
                                            // (init to leader last log index + 1)
    private Map<String, Integer> matchIndex; // key = node link id, value = idx of highest log entry known to be replicated on server
                                            // (init to 0, increases monotonically) 

    public State(Address timerAddress, Address graphAddress, Address logAddress, long seed, String selfLinkId,
            UnmodifiableSet<String> nodeLinkIds, AddressTransformer addressTransformer, Controller routerController) {
        Validate.notNull(timerAddress);
        Validate.notNull(graphAddress);
        Validate.notNull(logAddress);
        Validate.notNull(selfLinkId);
        Validate.notNull(nodeLinkIds);
        Validate.notNull(addressTransformer);
        Validate.notNull(routerController);
        Validate.isTrue(nodeLinkIds.contains(selfLinkId));
        Validate.isTrue(nodeLinkIds.size() >= 2);
        
        this.timerAddress = timerAddress;
        this.graphAddress = graphAddress;
        this.logAddress = logAddress;
        this.random = new Random(seed);
        this.selfLinkId = selfLinkId;
        Set<String> modifiedNodeLinks = new HashSet<>(nodeLinkIds);
        modifiedNodeLinks.remove(selfLinkId);
        this.otherNodeLinkIds = (UnmodifiableSet<String>) UnmodifiableSet.unmodifiableSet(new HashSet<String>(modifiedNodeLinks));
        this.addressTransformer = addressTransformer;
        this.routerController = routerController;

        currentTerm = 0;
        votedForLinkId = null; // can be null
        log = new LinkedList<>();

        commitIndex = 0;
        lastApplied = 0;

        nextIndex = new HashMap<>();
        matchIndex = new HashMap<>();
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

    public Random getRandom() {
        return random;
    }

    public String getSelfLinkId() {
        return selfLinkId;
    }

    public UnmodifiableSet<String> getOtherNodeLinkIds() {
        return otherNodeLinkIds;
    }

    public AddressTransformer getAddressTransformer() {
        return addressTransformer;
    }

    public Controller getRouterController() {
        return routerController;
    }

    public String nextRandomId() {
        long ret = ((long) random.nextInt()) << 32L | (long) counter;
        counter++;
        return "" + ret;
    }

    public int incrementCurrentTerm() {
        currentTerm++;
        return currentTerm;
    }
    
    public int getCurrentTerm() {
        return currentTerm;
    }

    public String getVotedForLinkId() {
        return votedForLinkId;
    }

    public void setVotedForLinkId(String votedForLinkId) {
        this.votedForLinkId = votedForLinkId; // can be null
    }


}
