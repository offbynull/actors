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
import org.apache.commons.lang3.Validate;

final class State {

    private final Random random;
    private final int maxIncomingLinks;
    private final int maxOutgoingLinks;
    private int counter = 0;
    private final Set<String> pendingOutgoingLinks; // pending outgoing links (outgoing linkids), connection not established yet
    private final Map<String, Address> outgoingLinks; // active outgoing links... key=outgoing linkid, value=address suffix
    private final Map<String, Address> incomingLinks; // active incoming links... key=incoming linkid, value=address suffix
    private final int maxCachedAddresses;
    private List<String> addressCache; // address to handler
    
    private final AddressTransformer addressTransformer;

    public State(
            long seed,
            int maxIncomingLinks,
            int maxOutgoingLinks,
            int maxCachedAddresses,
            Set<String> bootstrapLinkIds,
            AddressTransformer addressTransformer) {
        Validate.notNull(bootstrapLinkIds);
        Validate.notNull(addressTransformer);
        Validate.noNullElements(bootstrapLinkIds);
        Validate.isTrue(maxIncomingLinks >= 0);
        Validate.isTrue(maxOutgoingLinks >= 0);
        Validate.isTrue(maxCachedAddresses >= 0);
        Validate.isTrue(bootstrapLinkIds.size() <= maxCachedAddresses);
        random = new Random(seed);
        this.maxIncomingLinks = maxIncomingLinks;
        this.maxOutgoingLinks = maxOutgoingLinks;
        pendingOutgoingLinks = new HashSet<>();
        outgoingLinks = new HashMap<>();
        incomingLinks = new HashMap<>();
        this.maxCachedAddresses = maxCachedAddresses;
        this.addressCache = new ArrayList<>(maxCachedAddresses);
        this.addressCache.addAll(bootstrapLinkIds);
        
        this.addressTransformer = addressTransformer;
    }

    public String nextRandomId() {
        long ret = ((long) random.nextInt()) << 32L | (long) counter;
        counter++;
        return "" + ret;
    }

    public Set<String> getLinks() {
        Set<String> ret = new HashSet<>();
        ret.addAll(outgoingLinks.keySet());
        ret.addAll(incomingLinks.keySet());
        return ret;
    }

    public Set<String> getPendingOutgoingLinks() {
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

    public void addIncomingLink(String linkId, Address suffix) {
        Validate.notNull(linkId);
        Validate.notNull(suffix);
        Validate.isTrue(incomingLinks.size() < maxIncomingLinks);
        incomingLinks.put(linkId, suffix);
    }

    public void addOutgoingLink(String linkId, Address suffix) {
        Validate.notNull(linkId);
        Validate.notNull(suffix);
        Validate.isTrue(outgoingLinks.size() < maxOutgoingLinks);
        outgoingLinks.put(linkId, suffix);
    }

    public void addPendingOutgoingLink(String linkId) {
        Validate.notNull(linkId);
        pendingOutgoingLinks.add(linkId);
    }

    public void removeIncomingLink(String linkId) {
        Validate.notNull(linkId);
        Validate.isTrue(incomingLinks.remove(linkId) != null);
    }

    public void removeOutgoingLink(String linkId) {
        Validate.notNull(linkId);
        Validate.isTrue(outgoingLinks.remove(linkId) != null);
    }

    public void removePendingOutgoingLink(String linkId) {
        Validate.notNull(linkId);
        Validate.isTrue(pendingOutgoingLinks.remove(linkId));
    }
    
    public Address getIncomingLinkSuffix(String linkId) {
        Validate.notNull(linkId);
        return incomingLinks.get(linkId);
    }

    public boolean hasMoreCachedAddresses() {
        return !addressCache.isEmpty();
    }

    public String getNextCachedLinkId() {
        // Does not actually remove, takes top item and moves it to the bottom of the list (top item is returned)
        Iterator<String> it = addressCache.iterator();
        String ret = it.next();
        it.remove();
        addressCache.add(ret);
        return ret;
    }
    
    public Set<String> getCachedAddresses() {
        return new HashSet<>(addressCache);
    }

    public void addCachedLinkIds(Set<String> addresses) {
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
