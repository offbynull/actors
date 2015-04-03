package com.offbynull.peernetic.core.actors.reliableproxy;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.common.AddressUtils;
import static com.offbynull.peernetic.core.common.AddressUtils.SEPARATOR;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public final class RetryReceiveProxyCoroutine implements Coroutine {

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        StartRetryReceiveProxy startProxy = ctx.getIncomingMessage();
        String timerAddressPrefix = startProxy.getTimerPrefix();
        String dst = startProxy.getDestinationAddress();
        String self = ctx.getSelf();
        ReceiveGuidelineGenerator generator = startProxy.getGenerator();

        Map<Long, Object> cache = new HashMap<>();

        while (true) {
            cnt.suspend();

            String from = ctx.getSource();
            String to = ctx.getDestination();

            if (AddressUtils.isParent(timerAddressPrefix, from)) { // from timer
                // Get id of the cache item that we should remove
                String idStr = AddressUtils.relativize(self, ctx.getDestination());
                long id = Long.parseLong(idStr);

                // Remove id
                cache.remove(id);
            } else if (AddressUtils.isParent(dst, from)) { // outgoing resp
                // Get message
                Object resp = ctx.getIncomingMessage();

                // Get id for request
                String idStr = AddressUtils.getLastAddressElement(to);
                long id = Long.parseLong(idStr);
                
                // Add response to cache ... if id doesn't exist or if response has already been calculate for id, skip
                if (!cache.replace(id, null, resp)) {
                    continue;
                }
                
                // Pass response along with idStr in source address, so the destination can associate this response with a request
                ctx.addOutgoingMessage(idStr, dst, resp);
            } else { // incoming req
                // Get request
                Object req = ctx.getIncomingMessage();

                // Get id for request
                String idStr = AddressUtils.getLastAddressElement(from);
                long id = Long.parseLong(idStr);

                // Add request to cache ... if already exists, skip
                if (cache.putIfAbsent(id, null) != null) {
                    continue;
                }

                // Schedule removal from cache
                ReceiveGuideline guideline = generator.generate(req);

                Duration cacheWaitDuration = guideline.getCacheWaitDuration();
                String timerAddress = timerAddressPrefix + SEPARATOR + cacheWaitDuration.toMillis();
                ctx.addOutgoingMessage(idStr, timerAddress, "");

                // Pass request along with idStr in source address, so if the destination wants to respond it can
                ctx.addOutgoingMessage(idStr, dst, req);
            }
        }
    }

}
