/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.overlay.unstructured;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import org.apache.commons.lang3.Validate;

/**
 * Unstructured overlay configuration.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class UnstructuredOverlayConfig<A> {
    private Set<A> bootstrapAddresses = Collections.emptySet();
    private Random random = new SecureRandom();
    private int maxIncomingLinks = 30;
    private int maxOutgoingLinks = 30;
    private long incomingLinkExpireDuration = 60000L;
    private long outgoingLinkStaleDuration = 30000L;
    private long outgoingLinkExpireDuration = 60000L;
    private long cycleDuration = 10000L;
    private int maxOutgoingLinkAttemptsPerCycle = 3;
    private int serviceId = UnstructuredService.SERVICE_ID;

    /**
     * Get the set of bootstrap addresses (addresses to connect to on startup). Defaults to empty.
     * @return set of bootstrap addresses
     */
    public Collection<A> getBootstrapAddresses() {
        return bootstrapAddresses;
    }

    /**
     * Set the set of bootstrap addresses (addresses to connect to on startup).
     * @param bootstrapAddresses set of bootstrap addresses
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     */
    public void setBootstrapAddresses(Set<A> bootstrapAddresses) {
        Validate.notNull(bootstrapAddresses);
        this.bootstrapAddresses = bootstrapAddresses;
    }

    /**
     * Get the RNG to use for outgoing link secret generation.
     * @return RNG to use for outgoing link secret generation
     */
    public Random getRandom() {
        return random;
    }

    /**
     * Set the RNG to use for outgoing link secret generation.
     * @param random RNG to use for outgoing link secret generation
     * @throws NullPointerException if any numeric arguments are negative
     */
    public void setRandom(Random random) {
        Validate.notNull(random);
        this.random = random;
    }

    /**
     * Get the maximum number of incoming links allowed.
     * @return maximum number of incoming links allowed
     */
    public int getMaxIncomingLinks() {
        return maxIncomingLinks;
    }

    /**
     * Set the maximum number of incoming links allowed.
     * @param maxIncomingLinks maximum number of incoming links allowed
     * @throws IllegalArgumentException if any numeric arguments are negative
     */
    public void setMaxIncomingLinks(int maxIncomingLinks) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, maxIncomingLinks);
        this.maxIncomingLinks = maxIncomingLinks;
    }

    /**
     * Get the maximum number of incoming links allowed.
     * @return maximum number of incoming links allowed
     */
    public int getMaxOutgoingLinks() {
        return maxOutgoingLinks;
    }

    /**
     * Set the maximum number of outgoing links allowed. If {@code maxOutgoingLinkAttemptsPerCycle} is greater than this new value, it'll
     * be updated so that it matches the new value.
     * @param maxOutgoingLinks maximum number of outgoing links allowed
     * @throws IllegalArgumentException if any numeric arguments are negative
     */
    public void setMaxOutgoingLinks(int maxOutgoingLinks) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, maxOutgoingLinks);
        this.maxOutgoingLinks = maxOutgoingLinks;
        
        if (maxOutgoingLinkAttemptsPerCycle > maxOutgoingLinks) {
            maxOutgoingLinkAttemptsPerCycle = maxOutgoingLinks;
        }
    }

    /**
     * Get the amount of time to wait for an keep-alive before expiring an incoming link.
     * @return amount of time to wait for an keep-alive before expiring an incoming link
     */
    public long getIncomingLinkExpireDuration() {
        return incomingLinkExpireDuration;
    }

    /**
     * Get the amount of time to wait for an keep-alive before expiring an incoming link.
     * @param incomingLinkExpireDuration amount of time to wait for an keep-alive before expiring an incoming link
     * @throws IllegalArgumentException if any numeric arguments are negative
     */
    public void setIncomingLinkExpireDuration(long incomingLinkExpireDuration) {
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, incomingLinkExpireDuration);
        this.incomingLinkExpireDuration = incomingLinkExpireDuration;
    }

    /**
     * Get the amount of time to wait before sending a keep-alive for an outgoing link.
     * @return amount of time to wait before sending a keep-alive for an outgoing link
     */
    public long getOutgoingLinkStaleDuration() {
        return outgoingLinkStaleDuration;
    }

    /**
     * Set the amount of time to wait before sending a keep-alive for an outgoing link. If {@code outgoingLinkExpireDuration} is less than
     * this value, it'll be updated so that it matches the new value.
     * @param outgoingLinkStaleDuration the amount of time to wait before sending a keep-alive for an outgoing link
     * @throws IllegalArgumentException if any numeric arguments are negative
     */
    public void setOutgoingLinkStaleDuration(long outgoingLinkStaleDuration) {
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, outgoingLinkStaleDuration);
        this.outgoingLinkStaleDuration = outgoingLinkStaleDuration;
        if (outgoingLinkExpireDuration < outgoingLinkStaleDuration) {
            outgoingLinkExpireDuration = outgoingLinkStaleDuration;
        }
    }

    /**
     * Get the amount of time to wait before expiring an outgoing link.
     * @return amount of time to wait before expiring an outgoing link
     */
    public long getOutgoingLinkExpireDuration() {
        return outgoingLinkExpireDuration;
    }

    /**
     * Set the amount of time to wait before expiring an outgoing link. If {@code outgoingLinkStaleDuration} is greater than this value,
     * it'll be updated so that it matches the new value.
     * @param outgoingLinkExpireDuration the amount of time to wait before expiring an outgoing link
     * @throws IllegalArgumentException if any numeric arguments are negative
     */
    public void setOutgoingLinkExpireDuration(long outgoingLinkExpireDuration) {
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, outgoingLinkExpireDuration);
        this.outgoingLinkExpireDuration = outgoingLinkExpireDuration;
        
        if (outgoingLinkStaleDuration > outgoingLinkExpireDuration) {
            outgoingLinkStaleDuration = outgoingLinkExpireDuration;
        }
    }

    /**
     * Get the amount of time to wait per iteration.
     * @return amount of time to wait per iteration
     */
    public long getCycleDuration() {
        return cycleDuration;
    }

    /**
     * Set the amount of time to wait per iteration.
     * @param cycleDuration amount of time to wait per iteration
     * @throws IllegalArgumentException if any numeric arguments are negative
     */
    public void setCycleDuration(long cycleDuration) {
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, cycleDuration);
        this.cycleDuration = cycleDuration;
    }

    /**
     * Get the maximum number of outgoing links to create per cycle.
     * @return maximum number of outgoing links to create per cycle
     */
    public int getMaxOutgoingLinkAttemptsPerCycle() {
        return maxOutgoingLinkAttemptsPerCycle;
    }

    /**
     * Set the maximum number of outgoing links to create per cycle. If {@code maxOutgoingLinks} is less than this value, it'll be updated
     * so that it matches the new value.
     * @param maxOutgoingLinkAttemptsPerCycle maximum number of outgoing links to create per cycle
     * @throws IllegalArgumentException if any numeric arguments are negative
     */
    public void setMaxOutgoingLinkAttemptsPerCycle(int maxOutgoingLinkAttemptsPerCycle) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, maxOutgoingLinkAttemptsPerCycle);
        this.maxOutgoingLinkAttemptsPerCycle = maxOutgoingLinkAttemptsPerCycle;
        if (maxOutgoingLinks < maxOutgoingLinkAttemptsPerCycle) {
            maxOutgoingLinks = maxOutgoingLinkAttemptsPerCycle;
        }
    }

    /**
     * Get the RPC service ID.
     * @return RPC service ID
     */
    public int getServiceId() {
        return serviceId;
    }

    /**
     * Set the RPC service ID.
     * @param serviceId RPC service ID
     */
    public void setServiceId(int serviceId) {
        this.serviceId = serviceId;
    }
    
}
