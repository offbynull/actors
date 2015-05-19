package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.peernetic.core.shuttle.Address;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class State {
    private final Random random;
    private final int maxIncomingLinks;
    private final int maxOutgoingLinks;
    private int counter = 0;
    private final Set<Address> outgoingLinks;
    private final Set<Address> incomingLinks;
    
    public State(long seed, int maxIncomingLinks, int maxOutgoingLinks) {
        Validate.isTrue(maxIncomingLinks >= 0);
        Validate.isTrue(maxOutgoingLinks >= 0);
        random = new Random(seed);
        this.maxIncomingLinks = maxIncomingLinks;
        this.maxOutgoingLinks = maxOutgoingLinks;
        outgoingLinks = new HashSet<>();
        incomingLinks = new HashSet<>();
    }
    
    public long nextRandomId() {
        long ret = ((long) random.nextInt()) << 32L | (long) counter;
        counter++;
        return ret;
    }
    
    public Set<Address> getLinks() {
        Set<Address> ret = new HashSet<>();
        ret.addAll(outgoingLinks);
        ret.addAll(incomingLinks);
        return ret;
    }

    public int getMaxIncomingLinks() {
        return maxIncomingLinks;
    }

    public int getMaxOutgoingLinks() {
        return maxOutgoingLinks;
    }

    public boolean isIncomingLinksFull() {
        return incomingLinks.size() == maxIncomingLinks;
    }
    
    public boolean isOutgoingLinksFull() {
        return outgoingLinks.size() == maxOutgoingLinks;
    }
    
    public void addIncomingLink(Address link) {
        Validate.isTrue(incomingLinks.size() < maxIncomingLinks);
        incomingLinks.add(link);
    }
    
    public void addOutgoingLink(Address link) {
        Validate.isTrue(outgoingLinks.size() < maxOutgoingLinks);
        outgoingLinks.add(link);
    }

    public void removeIncomingLink(Address link) {
        Validate.isTrue(incomingLinks.remove(link));
    }
    
    public void removeOutgoingLink(Address link) {
        Validate.isTrue(outgoingLinks.remove(link));
    }
    
}
