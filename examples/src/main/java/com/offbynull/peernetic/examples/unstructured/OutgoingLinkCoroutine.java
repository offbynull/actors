package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkRequest;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkResponse;
import com.offbynull.peernetic.gateways.visualizer.StyleEdge;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

final class OutgoingLinkCoroutine implements Coroutine {
    private final Context mainCtx;
    private final IdGenerator idGenerator;
    private final String graphAddress;
    private final String address;
    private Instant lastResponseTime;

    public OutgoingLinkCoroutine(Context mainCtx, IdGenerator idGenerator, String graphAddress, String address, Instant currentTime) {
        Validate.notNull(mainCtx);
        Validate.notNull(idGenerator);
        Validate.notNull(address);
        Validate.notNull(graphAddress);
        Validate.notNull(currentTime);
        this.mainCtx = mainCtx;
        this.idGenerator = idGenerator;
        this.address = address;
        this.graphAddress = graphAddress;
        this.lastResponseTime = currentTime; // Initally set last resp time to current time. Required or else checks will fail because
                                             // this value will be null.
    }

    public Instant getLastResponseTime() {
        return lastResponseTime;
    }

    @Override
    public void run(Continuation cnt) throws Exception {
        mainCtx.addOutgoingMessage(graphAddress, new StyleEdge(mainCtx.getSelf(), address, "-fx-stroke: yellow"));
        
        while (true) {
            LinkRequest req = new LinkRequest(idGenerator.generateId());
            mainCtx.addOutgoingMessage(address, req);
            
            cnt.suspend();
            
            LinkResponse resp = mainCtx.getIncomingMessage();
            lastResponseTime = mainCtx.getTime();
            mainCtx.addOutgoingMessage(graphAddress, new StyleEdge(mainCtx.getSelf(), address, "-fx-stroke: green"));
        }
    }
    
}
