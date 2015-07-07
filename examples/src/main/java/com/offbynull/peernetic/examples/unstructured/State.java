package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.common.AddressTransformer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import org.apache.commons.lang3.Validate;

final class State {

    private final Random random;
    private final int maxIncomingLinks;
    private final int maxOutgoingLinks;
    private int counter = 0;
    private final Set<Address> pendingOutgoingLinks; // address to recver, connection not established yet
    private final Set<Address> outgoingLinks; // address to recver
    private final Map<Address, Address> incomingLinks; // address of sender -> incoming link subcoroutine source id
    private final int maxCachedAddresses;
    private List<Address> addressCache; // address to handler
    
    private final AddressTransformer addressTransformer;

    public State(
            long seed,
            int maxIncomingLinks,
            int maxOutgoingLinks,
            int maxCachedAddresses,
            Set<Address> bootstrapAddresses,
            AddressTransformer addressTransformer) {
        Validate.notNull(bootstrapAddresses);
        Validate.notNull(addressTransformer);
        Validate.noNullElements(bootstrapAddresses);
        Validate.isTrue(maxIncomingLinks >= 0);
        Validate.isTrue(maxOutgoingLinks >= 0);
        Validate.isTrue(maxCachedAddresses >= 0);
        Validate.isTrue(bootstrapAddresses.size() <= maxCachedAddresses);
        random = new Random(seed);
        this.maxIncomingLinks = maxIncomingLinks;
        this.maxOutgoingLinks = maxOutgoingLinks;
        pendingOutgoingLinks = new HashSet<>();
        outgoingLinks = new HashSet<>();
        incomingLinks = new HashMap<>();
        this.maxCachedAddresses = maxCachedAddresses;
        this.addressCache = new ArrayList<>(maxCachedAddresses);
        this.addressCache.addAll(bootstrapAddresses);
        
        this.addressTransformer = addressTransformer;
    }

    public String nextRandomId() {
        long ret = ((long) random.nextInt()) << 32L | (long) counter;
        counter++;
        return "" + ret;
    }

    public Set<Address> getLinks() {
        Set<Address> ret = new HashSet<>();
        ret.addAll(outgoingLinks);
        ret.addAll(incomingLinks.keySet());
        return ret;
    }

    public Set<Address> getPendingOutgoingLinks() {
        return new HashSet<>(pendingOutgoingLinks);
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
        Validate.notNull(sender);
        Validate.notNull(link);
        Validate.isTrue(incomingLinks.size() < maxIncomingLinks);
        incomingLinks.put(sender, link);
    }

    public void addOutgoingLink(Address recver) {
        Validate.notNull(recver);
        Validate.isTrue(outgoingLinks.size() < maxOutgoingLinks);
        outgoingLinks.add(recver);
    }

    public void addPendingOutgoingLink(Address recver) {
        Validate.notNull(recver);
        pendingOutgoingLinks.add(recver);
    }

    public void removeIncomingLink(Address sender) {
        Validate.notNull(sender);
        Validate.isTrue(incomingLinks.remove(sender) != null);
    }

    public void removeOutgoingLink(Address recver) {
        Validate.notNull(recver);
        Validate.isTrue(outgoingLinks.remove(recver));
    }

    public void removePendingOutgoingLink(Address recver) {
        Validate.notNull(recver);
        Validate.isTrue(pendingOutgoingLinks.remove(recver));
    }
    
    public Address getIncomingLink(Address sender) {
        Validate.notNull(sender);
        return incomingLinks.get(sender);
    }

    public boolean hasMoreCachedAddresses() {
        return !addressCache.isEmpty();
    }

    public Address getNextCachedAddress() {
        // Does not actually remove, takes top item and moves it to the bottom of the list (top item is returned)
        Iterator<Address> it = addressCache.iterator();
        Address ret = it.next();
        it.remove();
        addressCache.add(ret);
        return ret;
    }
    
    public Set<Address> getCachedAddresses() {
        return new HashSet<>(addressCache);
    }

    public void addCachedAddresses(Set<Address> addresses) {
        Validate.notNull(addresses);
        Validate.noNullElements(addresses);

        addressCache.addAll(addresses);
        int remainder = addressCache.size() - maxCachedAddresses;

        if (remainder > 0) {
            // if we have values left, take the tail end
            // put in new arraylist because sublist retains original list
            addressCache = new ArrayList<>(addressCache.subList(addressCache.size() - maxCachedAddresses, addressCache.size()));
        }
    }

    public AddressTransformer getAddressTransformer() {
        return addressTransformer;
    }
}
