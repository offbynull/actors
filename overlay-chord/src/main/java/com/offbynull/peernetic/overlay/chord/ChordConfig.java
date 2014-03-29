/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.overlay.chord;

import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.IdUtils;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.Validate;

/**
 * Configuration for {@link ChordOverlay}.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class ChordConfig<A> {
    private BigInteger limit;
    private Pointer<A> base;
    private Pointer<A> bootstrap;
    private Random random;
    private ChordOverlayListener<A> listener;
    private long rpcTimeoutDuration = 5000L;
    private int rpcMaxSendAttempts = 6;
    private long stabilizePeriod = 5000L;
    private long fixFingerPeriod = 5000L;
    private EndpointFinder<A> finder;

    private boolean locked;
    
    /**
     * Construct a {@link ChordConfig} object. By default, the base address is set to {@code 0} and there is no bootstrap address set
     * (meaning this node will seed the network).
     * @param address address of this node
     * @param limit id limit
     * @param finder finder to find addresses
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if creating default random number generator fails
     * @throws IllegalArgumentException if limit fails {@link IdUtils#ensureLimitPowerOfTwo(byte[]) }
     */
    public ChordConfig(A address, byte[] limit, EndpointFinder<A> finder) {
        Validate.notNull(address);
        Validate.notNull(limit);
        Validate.notNull(finder);
        IdUtils.ensureLimitPowerOfTwo(limit);
        
        
        int bitLen = IdUtils.getLimitBitLength(limit);
        try {
            random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        } catch (NoSuchAlgorithmException | NoSuchProviderException ex) {
            throw new IllegalStateException(ex);
        }
        Id id = IdUtils.generateRandomPowerOfTwoId(random, bitLen);
        
        
        this.limit = id.getLimitAsBigInteger();
        this.base = new Pointer<>(id, address);
        this.listener = new ChordOverlayListener<A>() {

            @Override
            public void stateUpdated(String event, Pointer<A> self, Pointer<A> predecessor, List<Pointer<A>> fingerTable,
                    List<Pointer<A>> successorTable) {
            }

            @Override
            public void failed(FailureMode failureMode) {
            }
        };
        
        this.finder = finder;
    }

    /**
     * Get this node's id and address.
     * @return this node's id and address
     */
    public Pointer<A> getBase() {
        return base;
    }

    /**
     * Set this node's id and address.
     * @param base id and address for this node
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if this object has been locked
     * @throws IllegalArgumentException if limit of {@code base} doesn't match limit set in this config, or if {@code base} has the same id
     * or address as the set bootstrap
     */
    public void setBase(Pointer<A> base) {
        Validate.validState(!locked);
        Validate.notNull(base);
        Validate.isTrue(base.getId().getLimitAsBigInteger().equals(limit));
        
        if (bootstrap != null) {
            Validate.isTrue(!bootstrap.getId().equals(base.getId()));
            Validate.isTrue(!bootstrap.getAddress().equals(base.getAddress()));
        }
        
        this.base = base;
    }

    /**
     * Get the node this node should connect to (can be {@code null}). If set to {@code null}, this node seeds the network.
     * @return node this node should connect to, if any
     */
    public Pointer<A> getBootstrap() {
        return bootstrap;
    }

    /**
     * Set the node this node should connect to, if any.
     * @param bootstrap id and address of the node to connect to
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if this object has been locked
     * @throws IllegalArgumentException if limit of {@code bootstrap} doesn't match limit set in this config, or if {@code bootstrap} has
     * the same id or address as the set base
     */
    public void setBootstrap(Pointer<A> bootstrap) {
        Validate.validState(!locked);
        if (bootstrap != null) {
            Validate.isTrue(base.getId().getLimitAsBigInteger().equals(limit));
            Validate.isTrue(!bootstrap.getId().equals(base.getId()));
            Validate.isTrue(!bootstrap.getAddress().equals(base.getAddress()));
        }
        
        this.bootstrap = bootstrap;
    }

    /**
     * Get the random number generator for this node.
     * @return RNG for this node
     */
    public Random getRandom() {
        return random;
    }

    /**
     * Set the random number generator for this node.
     * @param random rng
     * @throws NullPointerException if any argument is null
     * @throws IllegalStateException if this object has been locked
     */
    public void setRandom(Random random) {
        Validate.validState(!locked);
        Validate.notNull(random);
        this.random = random;
    }

    /**
     * Get the listener for this node.
     * @return listener for this node
     */
    public ChordOverlayListener<A> getListener() {
        return listener;
    }

    /**
     * Set the listener for this node.
     * @param listener listener
     * @throws NullPointerException if any argument is null
     * @throws IllegalStateException if this object has been locked
     */
    public void setListener(ChordOverlayListener<A> listener) {
        Validate.validState(!locked);
        Validate.notNull(listener);
        this.listener = listener;
    }

    /**
     * Get the RPC timeout duration.
     * @return RPC timeout duration
     */
    public long getRpcTimeoutDuration() {
        return rpcTimeoutDuration;
    }

    /**
     * Set the RPC timeout duration.
     * @param rpcTimeoutDuration timeout duration
     * @throws IllegalArgumentException if any numeric argument is non-positive {@code <= 0}
     * @throws IllegalStateException if this object has been locked
     */
    public void setRpcTimeoutDuration(long rpcTimeoutDuration) {
        Validate.validState(!locked);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, rpcTimeoutDuration);
        this.rpcTimeoutDuration = rpcTimeoutDuration;
    }

    /**
     * Get the RPC max send attempts duration.
     * @return RPC max send attempts
     */
    public int getRpcMaxSendAttempts() {
        return rpcMaxSendAttempts;
    }

    /**
     * Set the RPC max send attempts.
     * @param rpcMaxSendAttempts max send attempts
     * @throws IllegalArgumentException if any numeric argument is non-positive {@code <= 0}
     * @throws IllegalStateException if this object has been locked
     */
    public void setRpcMaxSendAttempts(int rpcMaxSendAttempts) {
        Validate.validState(!locked);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, rpcMaxSendAttempts);
        this.rpcMaxSendAttempts = rpcMaxSendAttempts;
    }

    /**
     * Get the amount of time to wait before calling stabilizing.
     * @return amount of time to wait before calling stabilizing
     */
    public long getStabilizePeriod() {
        return stabilizePeriod;
    }

    /**
     * Set the amount of time to wait before stabilizing.
     * @param stabilizePeriod amount of time to wait before stabilizing
     * @throws IllegalArgumentException if any numeric argument is non-positive {@code <= 0}
     * @throws IllegalStateException if this object has been locked
     */
    public void setStabilizePeriod(long stabilizePeriod) {
        Validate.validState(!locked);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, stabilizePeriod);
        this.stabilizePeriod = stabilizePeriod;
    }

    /**
     * Get the amount of time to wait before fixing a finger table entry.
     * @return amount of time to wait before fixing a finger table entry
     */
    public long getFixFingerPeriod() {
        return fixFingerPeriod;
    }


    /**
     * Set the amount of time to wait before fixing a finger table entry.
     * @param fixFingerPeriod amount of time to wait before fixing a finger table entry
     * @throws IllegalArgumentException if any numeric argument is non-positive {@code <= 0}
     * @throws IllegalStateException if this object has been locked
     */
    public void setFixFingerPeriod(long fixFingerPeriod) {
        Validate.validState(!locked);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, fixFingerPeriod);
        this.fixFingerPeriod = fixFingerPeriod;
    }

    /**
     * Get the endpoint finder used to extract addresses from sent messages.
     * @return endpoint finder
     */
    public EndpointFinder<A> getFinder() {
        return finder;
    }

    /**
     * Set the finder for this node.
     * @param finder finder
     * @throws NullPointerException if any argument is null
     * @throws IllegalStateException if this object has been locked
     */
    public void setFinder(EndpointFinder<A> finder) {
        Validate.validState(!locked);
        Validate.notNull(finder);
        this.finder = finder;
    }

    /**
     * Check to see if this config object is locked. If locked, no more changes are allowed.
     * @return {@code true} if locked, {@code false} otherwise
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * Locks this object so trying to set its properties will fail.
     */
    public void lock() {
        this.locked = true;
    }
}
