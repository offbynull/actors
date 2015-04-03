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
        IdExtractor idExtractor = startProxy.getIdExtractor();
        ReceiveGuidelineGenerator generator = startProxy.getGenerator();

        Map<String, RequestState> cache = new HashMap<>();

        while (true) {
            cnt.suspend();

            String from = ctx.getSource();

            if (AddressUtils.isParent(timerAddressPrefix, from)) { // from timer
                // Timer indicated that a cached item needs to be removed
                String id = AddressUtils.relativize(self, ctx.getDestination());
                cache.remove(id);
            } else if (AddressUtils.isParent(dst, from)) { // outgoing resp
                Object resp = ctx.getIncomingMessage();
                String id = idExtractor.getId(resp);

                // Add response to cache and send response
                RequestState reqState = cache.get(id);
                if (reqState != null) {
                    reqState.setResponse(resp);

                    String to = reqState.getSourceAddress();
                    ctx.addOutgoingMessage(to, resp);
                } else {
                    // Warn here that you're trying to override a response
                    // TODO log here
                }
            } else { // incoming req
                Object req = ctx.getIncomingMessage();
                String id = idExtractor.getId(req);

                RequestState reqState = cache.get(id);
                if (reqState == null) {
                    // If id isn't cached, add to cache + schedule removal from cache + pass to destination
                    reqState = new RequestState(from);
                    cache.put(id, reqState);
                    
                    ReceiveGuideline guideline = generator.generate(req);
                    Duration cacheWaitDuration = guideline.getCacheWaitDuration();
                    String timerAddress = timerAddressPrefix + SEPARATOR + cacheWaitDuration.toMillis();
                    ctx.addOutgoingMessage(id, timerAddress, "");

                    ctx.addOutgoingMessage(dst, req);
                } else if (reqState.getResponse() == null) {
                    // If id is cached but hasn't been responded to yet, log and ignore
                    // TODO hereLog here
                } else {
                    // if id is cached and we've already responed, send back the cached response
                    String to = reqState.getSourceAddress();
                    Object resp = reqState.getResponse();

                    ctx.addOutgoingMessage(to, resp);
                }
            }
        }
    }

    private static final class RequestState {

        private final String sourceAddress;
        private Object response;

        public RequestState(String sourceAddress) {
            this.sourceAddress = sourceAddress;
        }

        public String getSourceAddress() {
            return sourceAddress;
        }

        public Object getResponse() {
            return response;
        }

        public void setResponse(Object response) {
            this.response = response;
        }

    }

}
