package com.offbynull.peernetic.overlay.unstructured;

import com.offbynull.peernetic.rpc.Rpc;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.Validate;

class ScanCallable<A> implements Callable<Void> {
    
    private A bootstrap;
    private Rpc rpc;
    private int limit;
    private ScanListener<A> listener;
    private long minDelay;

    public ScanCallable(A bootstrap, Rpc rpc, int limit, long minDelay, ScanListener<A> listener) {
        Validate.notNull(bootstrap);
        Validate.notNull(rpc);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, limit);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, minDelay);
        Validate.notNull(listener);
        
        this.bootstrap = bootstrap;
        this.rpc = rpc;
        this.limit = limit;
        this.minDelay = minDelay;
        this.listener = listener;
    }

    @Override
    public Void call() throws Exception {
        List<A> bootstrapAddresses = Collections.singletonList(bootstrap);
        
        Set<A> pending = new LinkedHashSet<>();
        Set<A> accumulated = new LinkedHashSet<>();
        Set<A> oldAccumulated = new LinkedHashSet<>();
        Set<A> dead = new LinkedHashSet<>();
        
        while (true) {
            pending.addAll(bootstrapAddresses);
            accumulated.addAll(bootstrapAddresses);

            while (!pending.isEmpty() && accumulated.size() < limit) {
                long startTime = System.currentTimeMillis();
                
                A address = pending.iterator().next();
                pending.remove(address);

                OverlayService<A> node = (OverlayService<A>) rpc.accessService(address, OverlayService.SERVICE_ID, OverlayService.class);

                OverlayService.Information<A> info;
                try {
                    info = node.getInformation();
                } catch (RuntimeException re) {
                    dead.add(address);
                    continue;
                }

                Set<A> nodeNeighbours = new HashSet<>();
                nodeNeighbours.addAll(info.getOutgoingLinks());
                nodeNeighbours.addAll(info.getIncomingLinks());

                nodeNeighbours.removeAll(oldAccumulated); // remove all nodes that we already have visisted last iteration
                nodeNeighbours.removeAll(accumulated); // remove all nodes that we already have visited this iteration
                nodeNeighbours.removeAll(dead); // remove all nodes that we've designated as dead

                accumulated.addAll(nodeNeighbours);
                pending.addAll(nodeNeighbours);
                
                long endTime = System.currentTimeMillis();                
                long diffTime = endTime - startTime;
                
                long waitTime = Math.max(minDelay - diffTime, 0L); // just incase, hasn't been accurate enough at times
                Thread.sleep(waitTime);
            }

            oldAccumulated.clear();
            oldAccumulated.addAll(accumulated);

            listener.scanIterationComplete(new HashSet<>(accumulated));

            accumulated.clear();
            pending.clear();
            dead.clear();
        }
    }
    
}
