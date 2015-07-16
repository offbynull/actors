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

    private final Address timerAddress;
    private final Address graphAddress;
    private final Address logAddress;
    private final Random random;
    private final String selfLink;
    private final UnmodifiableSet<String> otherNodeLinks; // does not include self

    private final AddressTransformer addressTransformer;
    
    private final Controller routerController;

    public State(Address timerAddress, Address graphAddress, Address logAddress, long seed, String selfLink,
            UnmodifiableSet<String> nodeLinks, AddressTransformer addressTransformer, Controller routerController) {
        Validate.notNull(timerAddress);
        Validate.notNull(graphAddress);
        Validate.notNull(logAddress);
        Validate.notNull(selfLink);
        Validate.notNull(nodeLinks);
        Validate.notNull(addressTransformer);
        Validate.notNull(routerController);
        Validate.isTrue(nodeLinks.contains(selfLink));
        Validate.isTrue(nodeLinks.size() >= 2);
        
        this.timerAddress = timerAddress;
        this.graphAddress = graphAddress;
        this.logAddress = logAddress;
        this.random = new Random(seed);
        this.selfLink = selfLink;
        Set<String> modifiedNodeLinks = new HashSet<>(nodeLinks);
        modifiedNodeLinks.remove(selfLink);
        this.otherNodeLinks = (UnmodifiableSet<String>) UnmodifiableSet.unmodifiableSet(new HashSet<String>(modifiedNodeLinks));
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
        return selfLink;
    }

    public UnmodifiableSet<String> getOtherNodeLinks() {
        return otherNodeLinks;
    }

    public AddressTransformer getAddressTransformer() {
        return addressTransformer;
    }

    public Controller getRouterController() {
        return routerController;
    }


}
