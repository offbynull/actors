package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.peernetic.examples.common.request.ExternalMessageIdGenerator;
import com.offbynull.peernetic.examples.unstructured.externalmessages.QueryResponse;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkResponse;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkRequest;
import com.offbynull.peernetic.examples.unstructured.externalmessages.QueryRequest;
import com.offbynull.peernetic.examples.unstructured.internalmessages.Check;
import com.offbynull.peernetic.examples.unstructured.internalmessages.Start;
import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.visualizer.gateways.graph.AddEdge;
import com.offbynull.peernetic.visualizer.gateways.graph.AddNode;
import com.offbynull.peernetic.visualizer.gateways.graph.RemoveEdge;
import com.offbynull.peernetic.examples.unstructured.AddressCache.RetentionMode;
import com.offbynull.peernetic.visualizer.gateways.graph.MoveNode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;

public final class UnstructuredClientCoroutine implements Coroutine {

    private static final int MAX_OUTGOING_LINKS = 3;
    private static final int MAX_INCOMING_LINKS = 4;
    private static final Duration CHECK_DURATION = Duration.ofSeconds(1L);
    private static final Duration INCOMING_TIMEOUT = Duration.ofSeconds(10L);
    private static final Duration OUTGOING_TIMEOUT = Duration.ofSeconds(10L);
    private ExternalMessageIdGenerator idGenerator;
    private AddressCache addressCache;
    private Address timerAddressPrefix;
    private Address graphAddress;
    private Map<Address, ImmutablePair<CoroutineRunner, OutgoingLinkCoroutine>> outgoingLinks;
    private Map<Address, ImmutablePair<CoroutineRunner, IncomingLinkCoroutine>> incomingLinks;
    private Map<Class<?>, Consumer<Object>> handlerMap;
    
    private Context mainCtx;
    
    @Override
    public void run(Continuation cnt) throws Exception {
        mainCtx = (Context) cnt.getContext();
        
        Start start = mainCtx.getIncomingMessage();
        timerAddressPrefix = start.getTimerPrefix();
        graphAddress = start.getGraphAddress();
        
        idGenerator = new ExternalMessageIdGenerator(start.getRandom());
        addressCache = new AddressCache(256, start.getBootstrapAddresses(), RetentionMode.RETAIN_NEWEST);
        outgoingLinks = new HashMap<>();
        incomingLinks = new HashMap<>();
        
        handlerMap = new HashMap<>();
        handlerMap.put(Check.class, this::handleCheckFromTimer);
        handlerMap.put(LinkRequest.class, this::handleLinkRequest);
        handlerMap.put(LinkResponse.class, this::handleLinkResposne);
        handlerMap.put(QueryRequest.class, this::handleQueryRequest);
        handlerMap.put(QueryResponse.class, this::handleQueryResponse);
        
        mainCtx.addOutgoingMessage(
                timerAddressPrefix.appendSuffix("" + CHECK_DURATION.toMillis()),
                new Check());
        mainCtx.addOutgoingMessage(graphAddress, new AddNode(mainCtx.getSelf().toString()));
        mainCtx.addOutgoingMessage(graphAddress,
                new MoveNode(mainCtx.getSelf().toString(),
                        start.getRandom().nextInt(1400),
                        start.getRandom().nextInt(1400))
        );
        
        while (true) {
            cnt.suspend();

            Object msg = mainCtx.getIncomingMessage();
            Consumer<Object> consumer = handlerMap.get(msg.getClass());
            
            if (consumer != null) {
                consumer.accept(msg);
            } else {
                // TODO log error here
            }
        }
    }

    private void handleCheckFromTimer(Object msg) {
        Validate.isTrue(msg instanceof Check);
        
        Instant currentTime = mainCtx.getTime();
        
        // Remove outgoing links that have timed out
        Iterator<Entry<Address, ImmutablePair<CoroutineRunner, OutgoingLinkCoroutine>>> outIt = outgoingLinks.entrySet().iterator();
        while (outIt.hasNext()) {
            Entry<Address, ImmutablePair<CoroutineRunner, OutgoingLinkCoroutine>> entry = outIt.next();
            Instant sendTime = entry.getValue().getValue().getLastResponseTime();
            Duration duration = Duration.between(sendTime, currentTime);

            if (duration.compareTo(OUTGOING_TIMEOUT) > 0) {
                outIt.remove();
                mainCtx.addOutgoingMessage(graphAddress, new RemoveEdge(mainCtx.getSelf().toString(), entry.getKey().toString()));
            }
        }
        
        // Remove incoming links that have timed out
        Iterator<Entry<Address, ImmutablePair<CoroutineRunner, IncomingLinkCoroutine>>> inIt = incomingLinks.entrySet().iterator();
        while(inIt.hasNext()) {
            Entry<Address, ImmutablePair<CoroutineRunner, IncomingLinkCoroutine>> entry = inIt.next();
            Instant sendTime = entry.getValue().getValue().getLastRequestTime();
            Duration duration = Duration.between(sendTime, currentTime);
            
            if (duration.compareTo(INCOMING_TIMEOUT) > 0) {
                inIt.remove();
            }
        }

        // Reschedule check message
        mainCtx.addOutgoingMessage(
                timerAddressPrefix.appendSuffix("" + CHECK_DURATION.toMillis()),
                msg);

        // Add new outgoing links (if we have room available to do so)
        int newOutLinkSize = MAX_OUTGOING_LINKS - outgoingLinks.size();
        for (int i = 0; i < newOutLinkSize; i++) {
            Address address = addressCache.next();

            // If no more addresses in address cache, break out of loop
            if (address == null) {
                break;
            }
            
            // If we're already linked to the address in the cache, ignore
            if (outgoingLinks.containsKey(address) || incomingLinks.containsKey(address)) {
                continue;
            }

                // Add edge to graph
            mainCtx.addOutgoingMessage(graphAddress, new AddEdge(mainCtx.getSelf().toString(), address.toString()));
            
                // Start coroutine to attempt a link
            OutgoingLinkCoroutine coroutine = new OutgoingLinkCoroutine(mainCtx, idGenerator, graphAddress, address, currentTime);
            CoroutineRunner runner = new CoroutineRunner(coroutine);
            boolean stillRunning = runner.execute();
            Validate.isTrue(stillRunning);
            outgoingLinks.put(address, new ImmutablePair<>(runner, coroutine));
            
                // Send a query request
            mainCtx.addOutgoingMessage(address, new QueryRequest(idGenerator.generateId())); // send query 
        }
    }
    
    private void handleLinkRequest(Object msg) {
        Validate.isTrue(msg instanceof LinkRequest);
        LinkRequest req = (LinkRequest) msg;
        
        Address sourceAddress = mainCtx.getSource();
        
        ImmutablePair<CoroutineRunner, IncomingLinkCoroutine> entry = incomingLinks.get(sourceAddress);
        CoroutineRunner runner;
        if (entry == null) {
            // new incoming link
            if (incomingLinks.size() >= MAX_INCOMING_LINKS) {
                LinkResponse resp = new LinkResponse(req.getId(), false);
                mainCtx.addOutgoingMessage(sourceAddress, resp);
                return;
            }
            IncomingLinkCoroutine coroutine = new IncomingLinkCoroutine(mainCtx, sourceAddress);
            runner = new CoroutineRunner(coroutine);
            incomingLinks.put(sourceAddress, new ImmutablePair<>(runner, coroutine));
        } else {
            // maintaining existing incoming link
            runner = entry.getKey();
        }

        boolean stillRunning = runner.execute();
        Validate.isTrue(stillRunning);
    }
    
    private void handleLinkResposne(Object msg) {
        Validate.isTrue(msg instanceof LinkResponse);
        
        // A response to an outgoing link request
        Address sourceAddress = mainCtx.getSource();

        ImmutablePair<CoroutineRunner, OutgoingLinkCoroutine> coroutine = outgoingLinks.get(sourceAddress);
        if (coroutine == null) {
            return;
        }

        boolean stillRunning = coroutine.getKey().execute();
        if (!stillRunning) {
            outgoingLinks.remove(sourceAddress);
            mainCtx.addOutgoingMessage(graphAddress, new RemoveEdge(mainCtx.getSelf().toString(), sourceAddress.toString()));
        }
    }
    
    private void handleQueryRequest(Object msg) {
        Validate.isTrue(msg instanceof QueryRequest);
        
        // Incoming query request
        Address sourceAddress = mainCtx.getSource();

        QueryRequest req = (QueryRequest) msg;
        long id = req.getId();

        ArrayList<Address> links = new ArrayList<>(MAX_INCOMING_LINKS + MAX_OUTGOING_LINKS);
        links.addAll(incomingLinks.keySet());
        links.addAll(outgoingLinks.keySet());
        QueryResponse resp = new QueryResponse(id, links);

        mainCtx.addOutgoingMessage(sourceAddress, resp);
    }
    
    private void handleQueryResponse(Object msg) {
        Validate.isTrue(msg instanceof QueryResponse);
        
        // A response to an outgoing query request
        QueryResponse resp = (QueryResponse) msg;
        List<Address> addresses = new ArrayList<>(resp.getLinks());

        addressCache.addAll(addresses);
    }
}
