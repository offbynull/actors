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
    
    public ChordConfig(A address, byte[] limit, EndpointFinder<A> finder) throws NoSuchAlgorithmException, NoSuchProviderException {
        Validate.notNull(address);
        Validate.notNull(limit);
        Validate.notNull(finder);
        IdUtils.ensureLimitPowerOfTwo(limit);
        
        
        int bitLen = IdUtils.getLimitBitLength(limit);
        random = SecureRandom.getInstance("SHA1PRNG", "SUN");
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

    public Pointer<A> getBase() {
        return base;
    }

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

    public Pointer<A> getBootstrap() {
        return bootstrap;
    }

    public void setBootstrap(Pointer<A> bootstrap) {
        Validate.validState(!locked);
        if (bootstrap != null) {
            Validate.isTrue(base.getId().getLimitAsBigInteger().equals(limit));
            Validate.isTrue(!bootstrap.getId().equals(base.getId()));
            Validate.isTrue(!bootstrap.getAddress().equals(base.getAddress()));
        }
        
        this.bootstrap = bootstrap;
    }

    public Random getRandom() {
        return random;
    }

    public void setRandom(Random random) {
        Validate.validState(!locked);
        Validate.notNull(random);
        this.random = random;
    }

    public ChordOverlayListener<A> getListener() {
        return listener;
    }

    public void setListener(ChordOverlayListener<A> listener) {
        Validate.validState(!locked);
        Validate.notNull(listener);
        this.listener = listener;
    }

    public long getRpcTimeoutDuration() {
        return rpcTimeoutDuration;
    }

    public void setRpcTimeoutDuration(long rpcTimeoutDuration) {
        Validate.validState(!locked);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, rpcTimeoutDuration);
        this.rpcTimeoutDuration = rpcTimeoutDuration;
    }

    public int getRpcMaxSendAttempts() {
        return rpcMaxSendAttempts;
    }

    public void setRpcMaxSendAttempts(int rpcMaxSendAttempts) {
        Validate.validState(!locked);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, rpcMaxSendAttempts);
        this.rpcMaxSendAttempts = rpcMaxSendAttempts;
    }

    public long getStabilizePeriod() {
        return stabilizePeriod;
    }

    public void setStabilizePeriod(long stabilizePeriod) {
        Validate.validState(!locked);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, stabilizePeriod);
        this.stabilizePeriod = stabilizePeriod;
    }

    public long getNotifyPeriod() {
        return fixFingerPeriod;
    }

    public void setFixFingerPeriod(long fixFingerPeriod) {
        Validate.validState(!locked);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, fixFingerPeriod);
        this.fixFingerPeriod = fixFingerPeriod;
    }

    public EndpointFinder<A> getFinder() {
        return finder;
    }

    public void setFinder(EndpointFinder<A> finder) {
        Validate.validState(!locked);
        Validate.notNull(finder);
        this.finder = finder;
    }

    public boolean isLocked() {
        return locked;
    }

    public void lock() {
        this.locked = true;
    }
    
}
