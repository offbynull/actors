package com.offbynull.peernetic.eventframework.impl.network.address;

import com.offbynull.peernetic.eventframework.impl.generic.ThreadedExecOutgoingEvent;
import com.offbynull.peernetic.eventframework.impl.generic.ThreadedExecResultIncomingEvent;
import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.event.EventUtils;
import com.offbynull.peernetic.eventframework.processor.FinishedProcessResult;
import com.offbynull.peernetic.eventframework.processor.OngoingProcessResult;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;
import com.offbynull.peernetic.eventframework.processor.Processor;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;

public final class AddressResolveProcessor
        implements Processor<AddressResolvedIncomingEvent> {

    private State state;
    private long trackedId;
    private String host;

    public AddressResolveProcessor(String host) {
        if (host == null) {
            throw new NullPointerException();
        }
        
        host = host.trim();
        
        if (host.isEmpty()) {
            throw new IllegalArgumentException();
        }
        
        this.host = host;
        state = State.INIT;
    }
    
    @Override
    public ProcessResult<AddressResolvedIncomingEvent> process(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen) {
        switch (state) {
            case INIT:
                return processInitState(timestamp, event, trackedIdGen);
            case RESOLVING:
                return processResolvingState(timestamp, event, trackedIdGen);
            case FINISHED:
                return processFinishedState(timestamp, event, trackedIdGen);
            default:
                throw new IllegalStateException();
        }
    }

    private ProcessResult<AddressResolvedIncomingEvent> processInitState(
            long timestamp, IncomingEvent event,
            TrackedIdGenerator trackedIdGen) {
        trackedId = trackedIdGen.getNextId();
        
        InternalCallable callable = new InternalCallable();
        ThreadedExecOutgoingEvent teoe = new ThreadedExecOutgoingEvent(
                callable, trackedId);
        
        state = State.RESOLVING;
        
        return new OngoingProcessResult<>(teoe);
    }

    private ProcessResult<AddressResolvedIncomingEvent> processResolvingState(
            long timestamp, IncomingEvent event,
            TrackedIdGenerator trackedIdGen) {
        EventUtils.throwProcessorExceptionOnError(event, trackedId);
        
        ThreadedExecResultIncomingEvent terie = EventUtils.testAndConvert(
                event, trackedId, ThreadedExecResultIncomingEvent.class);
        
        @SuppressWarnings("unchecked")
        Set<ResolvedAddress> resAddrs =
                (Set<ResolvedAddress>) terie.getResult();
        
        state = State.FINISHED;
        
        AddressResolvedIncomingEvent ie =
                new AddressResolvedIncomingEvent(resAddrs);
        return new FinishedProcessResult<>(ie);
    }

    private ProcessResult<AddressResolvedIncomingEvent> processFinishedState(
            long timestamp, IncomingEvent event,
            TrackedIdGenerator trackedIdGen) {
        throw new IllegalArgumentException();
    }
    
    private final class InternalCallable
            implements Callable<Collection<ResolvedAddress>> {
        
        @Override
        public Collection<ResolvedAddress> call() throws Exception {
            InetAddress[] addrs = InetAddress.getAllByName(host);
            
            Set<ResolvedAddress> resolvedAddresses = new LinkedHashSet<>();
            for (InetAddress addr : addrs) {
                String hostname = addr.getCanonicalHostName();
                String ip = addr.getHostAddress();
                
                ResolvedAddress resAddr = new ResolvedAddress(hostname, ip);
                resolvedAddresses.add(resAddr);
            }
            
            return Collections.unmodifiableSet(resolvedAddresses);
        }
    }
    
    private enum State {
        INIT,
        RESOLVING,
        FINISHED
    }
}
