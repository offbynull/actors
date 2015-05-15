package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkRequest;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkResponse;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

final class IncomingLinkCoroutine implements Coroutine {
    private final Context mainCtx;
    private final Address address;
    private Instant lastRequestTime;

    public IncomingLinkCoroutine(Context mainCtx, Address address) {
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
            LinkResponse resp = new LinkResponse(req.getId(), true);
            mainCtx.addOutgoingMessage(address, resp);
            cnt.suspend();
        }
    }
    
}
