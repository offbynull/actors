package com.offbynull.overlay.unstructured;

import com.offbynull.rpc.RpcInvokeKeys;
import com.offbynull.rpc.invoke.InvokeThreadInformation;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.commons.lang3.Validate;

public final class OverlayServiceImplementation<A> implements OverlayService<A> {
    private PassiveExpiringMap<A, ByteBuffer> boundMap;
    private int maxBind;
    private Lock lock;

    public OverlayServiceImplementation(int maxBind, long timeToLive, Map<A, ByteBuffer> boundMap, Lock boundMapLock) {
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, maxBind);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, timeToLive);
        Validate.isTrue(boundMap.isEmpty());
        Validate.notNull(boundMapLock);

        this.boundMap = new PassiveExpiringMap<>(timeToLive, TimeUnit.MILLISECONDS, boundMap);
        this.lock = boundMapLock;
        this.maxBind = maxBind;
    }

    @Override
    public Information<A> getInformation() {
        lock.lock();
        try {
            return new Information<>(boundMap.keySet(), maxBind); // makes a copy of boundMap's keyset so this should be fine
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean join(byte[] secret) {
        Validate.inclusiveBetween(SECRET_SIZE, SECRET_SIZE, secret.length);

        A from = InvokeThreadInformation.getInfo(RpcInvokeKeys.FROM_ADDRESS);
        
        lock.lock();
        try {
            if (boundMap.size() == maxBind) {
                return false;
            }
            boundMap.put(from, ByteBuffer.wrap(Arrays.copyOf(secret, secret.length)));
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void unjoin(byte[] secret) {
        A from = InvokeThreadInformation.getInfo(RpcInvokeKeys.FROM_ADDRESS);
        
        ByteBuffer buffer;
        lock.lock();
        try {
            buffer = boundMap.get(from);
        } finally {
            lock.unlock();
        }
        
        if (buffer.equals(ByteBuffer.wrap(secret))) {
            boundMap.remove(from);
        }
    }

    @Override
    public boolean keepAlive(byte[] secret) {
        A from = InvokeThreadInformation.getInfo(RpcInvokeKeys.FROM_ADDRESS);
        
        ByteBuffer buffer;
        lock.lock();
        try {
            buffer = boundMap.get(from);
        } finally {
            lock.unlock();
        }
        
        if (buffer != null && buffer.equals(ByteBuffer.wrap(secret))) {
            boundMap.put(from, ByteBuffer.wrap(Arrays.copyOf(secret, secret.length)));
            return true;
        }
        
        return false;
    }
}
