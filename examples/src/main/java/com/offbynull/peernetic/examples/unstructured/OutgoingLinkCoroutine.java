package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkRequest;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkResponse;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

final class OutgoingLinkCoroutine implements Coroutine {
    private final Context mainCtx;
    private final IdGenerator idGenerator;
    private final String address;
    private Instant lastResponseTime;

    public OutgoingLinkCoroutine(Context mainCtx, IdGenerator idGenerator, String address, Instant currentTime) {
        Validate.notNull(mainCtx);
        Validate.notNull(idGenerator);
        Validate.notNull(address);
        Validate.notNull(currentTime);
        this.mainCtx = mainCtx;
        this.idGenerator = idGenerator;
        this.address = address;
        this.lastResponseTime = currentTime; // Initally set last resp time to current time. Required or else checks will fail because
                                             // this value will be null.
    }

    public Instant getLastResponseTime() {
        return lastResponseTime;
    }

    @Override
    public void run(Continuation cnt) throws Exception {
        while (true) {
            LinkRequest req = new LinkRequest(idGenerator.generateId());
            mainCtx.addOutgoingMessage(address, req);
            System.out.println("SENT REQUEST to " + address);
            
            cnt.suspend();
            
            LinkResponse resp = mainCtx.getIncomingMessage();
            lastResponseTime = mainCtx.getTime();
            System.out.println("GOT RESPONSE");
        }
    }
    
}
