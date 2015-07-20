package com.offbynull.peernetic.examples.raft;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.Controller;
import java.util.HashSet;
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
    
    private int term = 0;

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

    public String getSelfLink() {
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

    public int incrementTerm() {
        term++;
        return term;
    }
    
    public int getTerm() {
        return term;
    }
    
}
