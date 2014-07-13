package com.offbynull.peernetic.demo;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.demo.messages.internal.QueryNextAddress;
import com.offbynull.peernetic.demo.messages.internal.StartJoin;
import com.offbynull.peernetic.demo.messages.internal.StartSeed;
import com.offbynull.peernetic.fsm.FiniteStateMachine;
import com.offbynull.peernetic.fsm.StateHandler;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashSet;
import org.apache.commons.lang3.Validate;

public final class UnstructuredClient<A> {

    private static final String INITIAL_STATE = "INITIAL";
    private static final String ACTIVE_STATE = "ACTIVE";
    
    private EndpointDirectory<A> endpointDirectory;
    private EndpointScheduler endpointScheduler;
    private LinkedHashSet<A> addressCache;
    private LinkedHashSet<A> joinedNodes;
    private Endpoint selfEndpoint;

    public UnstructuredClient(EndpointDirectory<A> endpointDirectory, EndpointScheduler endpointScheduler) {
        Validate.notNull(endpointDirectory);
        Validate.notNull(endpointScheduler);
        
        this.endpointDirectory = endpointDirectory;
        this.endpointScheduler = endpointScheduler;
        this.addressCache = new LinkedHashSet<>();
    }

    @StateHandler(INITIAL_STATE)
    public void handleInitialSeed(String state, FiniteStateMachine fsm, Instant instant, StartSeed<A> message, Object param) {
        
    }

    @StateHandler(INITIAL_STATE)
    public void handleInitialJoin(String state, FiniteStateMachine fsm, Instant instant, StartJoin message, Object param) {
        selfEndpoint = message.getSelfEndpoint();
        addressCache = new LinkedHashSet<>(message.getBootstrapAddresses());
        joinedNodes = new LinkedHashSet<>(message.getBootstrapAddresses());
        
        fsm.switchStateAndProcess(ACTIVE_STATE, instant, new QueryNextAddress(), null);
    }

    @StateHandler(ACTIVE_STATE)
    public void handleJoining(String state, FiniteStateMachine fsm, Instant instant, QueryNextAddress message, Object param) {
        Iterator<A> addressCacheIt = addressCache.iterator();
        
        A address = addressCacheIt.next();
        addressCacheIt.remove();
        
        Endpoint endpoint = endpointDirectory.lookup(address);
        endpoint.send(selfEndpoint, joinmsg);
        
        endpointScheduler.scheduleMessage(Duration.ofSeconds(5L), selfEndpoint, selfEndpoint, new QueryNextAddress());
    }

    @StateHandler(ACTIVE_STATE)
    public void handleJoining(String state, FiniteStateMachine fsm, Instant instant, QueryNextAddress message, Object param) {
        Iterator<A> addressCacheIt = addressCache.iterator();
        
        A address = addressCacheIt.next();
        addressCacheIt.remove();
        
        Endpoint endpoint = endpointDirectory.lookup(address);
        endpoint.send(selfEndpoint, message);
        
        endpointScheduler.scheduleMessage(Duration.ofSeconds(5L), selfEndpoint, selfEndpoint, new QueryNextAddress());
    }
}
