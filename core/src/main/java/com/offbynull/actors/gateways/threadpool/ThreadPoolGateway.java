/*
 * Copyright (c) 2018, Kasra Faghihi, All rights reserved.
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
package com.offbynull.actors.gateways.threadpool;

import com.offbynull.actors.gateway.Gateway;
import com.offbynull.actors.shuttle.Shuttle;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

/**
 * {@link Gateway} that pushes messages to a thread pool for processing.
 * @author Kasra Faghihi
 */
public final class ThreadPoolGateway implements Gateway {

    private final ThreadPoolExecutor threadPool;
    private final ThreadPoolShuttle shuttle;    
    private final ConcurrentHashMap<String, Shuttle> outShuttles;    

    /**
     * Create a {@link ThreadPoolGateway} instance. Equivalent to calling {@code create(prefix, payloadTypes, threads, threads) }.
     * @param prefix address prefix for this gateway
     * @param payloadTypes supported message payload types (mapped to message processors)
     * @param threads number of threads in thread pool
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if {@code min <= 0} or if {@code max < min}
     * @return new thread pool gateway
     */
    public static ThreadPoolGateway create(String prefix, Map<Class<?>, ThreadPoolProcessor> payloadTypes, int threads) {
        Validate.notNull(prefix);
        Validate.isTrue(threads > 0);

        ThreadPoolGateway gateway = new ThreadPoolGateway(prefix, payloadTypes, threads, threads);
        return gateway;
    }

    /**
     * Create a {@link ThreadPoolGateway} instance.
     * @param prefix address prefix for this gateway
     * @param payloadTypes supported message payload types (mapped to message processors)
     * @param minThreads minimum number thread pool threads
     * @param maxThreads maximum number thread pool threads
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if {@code min <= 0} or if {@code max < min}
     * @return new thread pool gateway
     */
    public static ThreadPoolGateway create(String prefix, Map<Class<?>, ThreadPoolProcessor> payloadTypes, int minThreads, int maxThreads) {
        Validate.notNull(prefix);
        Validate.notNull(payloadTypes);
        Validate.noNullElements(payloadTypes.keySet());
        Validate.noNullElements(payloadTypes.values());
        Validate.isTrue(minThreads > 0);
        Validate.isTrue(maxThreads >= minThreads);

        ThreadPoolGateway gateway = new ThreadPoolGateway(prefix, payloadTypes, minThreads, maxThreads);
        return gateway;
    }
    
    private ThreadPoolGateway(String prefix, Map<Class<?>, ThreadPoolProcessor> payloadTypes, int minThreads, int maxThreads) {
        Validate.notNull(prefix);
        Validate.notNull(payloadTypes);
        Validate.noNullElements(payloadTypes.keySet());
        Validate.noNullElements(payloadTypes.values());
        Validate.isTrue(minThreads >= 0);
        Validate.isTrue(maxThreads >= 0);

        // https://stackoverflow.com/a/19528305
        // If you have a different min and max, there's a gotcha where using a normal LinkedBlockingQueue won't extend past the minimum.
        // This is a hack to make it go past the minimum.
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>() {
            @Override
            public boolean offer(Runnable e) {
                return isEmpty() ? super.offer(e) : false;
            }
        };

        threadPool = new ThreadPoolExecutor(
                minThreads,
                maxThreads,
                1L,
                TimeUnit.SECONDS,
                queue,
                new BasicThreadFactory.Builder()
                        .namingPattern(prefix + " worker %d")
                        .daemon(true)
                        .build()
        );
        threadPool.setRejectedExecutionHandler((Runnable r, ThreadPoolExecutor executor) -> {
            try {
                executor.getQueue().put(r);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        outShuttles = new ConcurrentHashMap<>();
        shuttle = new ThreadPoolShuttle(prefix, threadPool, payloadTypes, outShuttles);
    }

    @Override
    public Shuttle getIncomingShuttle() {
        if (threadPool.isShutdown()) {
            throw new IllegalStateException();
        }

        return shuttle;
    }

    @Override
    public void addOutgoingShuttle(Shuttle shuttle) {
        Validate.notNull(shuttle);
        if (threadPool.isShutdown()) {
            throw new IllegalStateException();
        }

        String shuttlePrefix = shuttle.getPrefix();
        outShuttles.put(shuttlePrefix, shuttle);
    }

    @Override
    public void removeOutgoingShuttle(String shuttlePrefix) {
        Validate.notNull(shuttlePrefix);
        if (threadPool.isShutdown()) {
            throw new IllegalStateException();
        }
        
        outShuttles.remove(shuttlePrefix);
    }

    @Override
    public void join() throws InterruptedException {
        threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }

    @Override
    public void close() {
        threadPool.shutdownNow();
    }
}
