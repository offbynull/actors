package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.peernetic.core.shuttle.Address;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class State {
    private final Random random;
    private final int maxIncomingLinks;
    private final int maxOutgoingLinks;
    private int counter = 0;
    private final Set<Address> outgoingLinks; // address to recver
    private final Map<Address, Address> incomingLinks; // address of sender -> incoming link subcoroutine source id
    
    public State(long seed, int maxIncomingLinks, int maxOutgoingLinks) {
        Validate.isTrue(maxIncomingLinks >= 0);
        Validate.isTrue(maxOutgoingLinks >= 0);
        random = new Random(seed);
        this.maxIncomingLinks = maxIncomingLinks;
        this.maxOutgoingLinks = maxOutgoingLinks;
        outgoingLinks = new HashSet<>();
        incomingLinks = new HashMap<>();
    }
    
    public long nextRandomId() {
        long ret = ((long) random.nextInt()) << 32L | (long) counter;
        counter++;
        return ret;
    }
    
    public Set<Address> getLinks() {
        Set<Address> ret = new HashSet<>();
        ret.addAll(outgoingLinks);
        ret.addAll(incomingLinks.keySet());
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
    
    public void addIncomingLink(Address sender, Address link) {
        Validate.isTrue(incomingLinks.size() < maxIncomingLinks);
        incomingLinks.put(sender, link);
    }
    
    public void addOutgoingLink(Address recver) {
        Validate.isTrue(outgoingLinks.size() < maxOutgoingLinks);
        outgoingLinks.add(recver);
    }

    public void removeIncomingLink(Address sender) {
        Validate.isTrue(incomingLinks.remove(sender) != null);
    }
    
    public void removeOutgoingLink(Address recver) {
        Validate.isTrue(outgoingLinks.remove(recver));
    }
    
}
