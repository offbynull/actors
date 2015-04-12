package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkRequest;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkResponse;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

final class IncomingLinkCoroutine implements Coroutine {
    private final Context mainCtx;
    private final String address;
    private Instant lastRequestTime;

    public IncomingLinkCoroutine(Context mainCtx, String address) {
        Validate.notNull(mainCtx);
        Validate.notNull(address);
        this.mainCtx = mainCtx;
        this.address = address;
    }

    public Instant getLastRequestTime() {
        return lastRequestTime;
    }

    @Override
    public void run(Continuation cnt) throws Exception {
        while (true) {
            LinkRequest req = mainCtx.getIncomingMessage();
            lastRequestTime = mainCtx.getTime();
            System.out.println("GOT REQUEST");
            LinkResponse resp = new LinkResponse(req.getId());
            mainCtx.addOutgoingMessage(address, resp);
            System.out.println("SENT RESPONSE");
            cnt.suspend();
        }
    }
    
}
