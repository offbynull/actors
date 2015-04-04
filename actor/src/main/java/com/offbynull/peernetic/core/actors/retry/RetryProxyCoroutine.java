package com.offbynull.peernetic.core.actors.retry;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.ProxyHelper;
import com.offbynull.peernetic.core.actor.ProxyHelper.ForwardInformation;
import com.offbynull.peernetic.core.common.AddressUtils;
import static com.offbynull.peernetic.core.common.AddressUtils.SEPARATOR;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.Validate;

public final class RetryProxyCoroutine implements Coroutine {

    private static final String REMOVE_INBOUND_TIMER_MSG = "inbound";
    private static final String EXECUTE_OUTBOUND_TIMER_MSG = "outbound";

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        StartRetryProxy startProxy = ctx.getIncomingMessage();
        SendGuidelineGenerator sgGen = startProxy.getSendGuidelineGenerator();
        ReceiveGuidelineGenerator rgGen = startProxy.getReceiveGuidelineGenerator();
        IdExtractor idExtractor = startProxy.getIdExtractor();
        String timerPrefix = startProxy.getTimerPrefix();
        String actorPrefix = startProxy.getActorPrefix();
        String self = ctx.getSelf();

        ProxyHelper proxyHelper = new ProxyHelper(ctx, actorPrefix);
        Map<String, OutboundRequestState> outboundRequests = new HashMap<>();
        Map<String, InboundRequestState> inboundRequests = new HashMap<>();

        while (true) {
            cnt.suspend();

            String from = ctx.getSource();

            if (proxyHelper.isMessageFrom(timerPrefix)) {
                String id = AddressUtils.relativize(self, ctx.getDestination());
                
                switch ((String) ctx.getIncomingMessage()) {
                    case EXECUTE_OUTBOUND_TIMER_MSG: {
                        // Timer indicates that a SendCoroutine should run another cycle, thereby sending another message out
                        OutboundRequestState outReqState = outboundRequests.get(id);

                        if (outReqState != null && !outReqState.isResponseArrived()) {
                            CoroutineRunner sender = outReqState.getCoroutineRunner();
                            boolean stillRunning = sender.execute();
                            if (!stillRunning) {
                                inboundRequests.remove(id);
                            }
                        }
                        break;
                    }
                    case REMOVE_INBOUND_TIMER_MSG: {
                        // Timer indicates that an inbound request needs to be removed
                        inboundRequests.remove(id);
                        break;
                    }
                    default:
                        throw new IllegalStateException();
                }
            } else if (proxyHelper.isMessageFromActor()) {
                // Outgoing message
                Object msg = ctx.getIncomingMessage();
                String id = idExtractor.getId(msg);

                // Make sure the actor isn't trying to send a request with an id thats the same as a request we've already sent and cached
                Validate.isTrue(!outboundRequests.containsKey(id));

                InboundRequestState inReqState = inboundRequests.get(id);
                if (inReqState != null) {
                    // This is a response from the actor to an inbound request. Make sure we haven't processed the respone already before
                    // sending it out
                    Validate.isTrue(inReqState.getResponse() == null);
                    proxyHelper.forwardToOutside(inReqState);
                    continue;
                }

                // This is a new outbound request, process it
                ForwardInformation forwardInfo = proxyHelper.generateOutboundForwardInformation();
                SendGuideline guideline = sgGen.generate(msg);

                SenderCoroutine senderCoroutine = new SenderCoroutine(ctx, timerPrefix, guideline, forwardInfo, msg, id);
                CoroutineRunner sender = new CoroutineRunner(senderCoroutine);
                OutboundRequestState outReqState = new OutboundRequestState(from, sender);

                Validate.isTrue(!inboundRequests.containsKey(id)); // make sure the id doesn't conflict with inbound request id

                outboundRequests.put(id, outReqState);
                boolean stillRunning = sender.execute();
                Validate.isTrue(stillRunning); // should never fail on first execution
            } else {
                // Incoming message
                Object msg = ctx.getIncomingMessage();
                String id = idExtractor.getId(msg);

                OutboundRequestState outReqState = outboundRequests.get(id);
                if (outReqState != null) {
                    // This is a response to an outbound request, forward it to the actor if we haven't already received a response
                    if (!outReqState.isResponseArrived()) {
                        outReqState.setResponseArrived(true);
                        proxyHelper.forwardToActor(msg);
                    }
                    continue;
                }

                InboundRequestState inReqState = inboundRequests.get(id);
                if (inReqState != null) {
                    // This is an inbound request that we've already received and processed. If we have the response available then ship it
                    // out, otherwise skip
                    Object response = inReqState.getResponse();
                    if (response != null) {
                        proxyHelper.forwardToOutside(response);
                    }
                    continue;
                }

                // This is a new inbound request, so process it and forward it to the actor
                inReqState = new InboundRequestState(ctx.getSource());
                inboundRequests.put(id, inReqState);

                ReceiveGuideline guideline = rgGen.generate(msg);
                String timerAddress = timerPrefix + SEPARATOR + guideline.getWaitDuration().toMillis();
                ctx.addOutgoingMessage(id, timerAddress, REMOVE_INBOUND_TIMER_MSG);

                proxyHelper.forwardToActor(msg);
            }
        }
    }

    private static final class SenderCoroutine implements Coroutine {

        private Context context;
        private String timerPrefix;
        private SendGuideline guideline;
        private ForwardInformation forwardInfo;
        private Object msg;
        private String id;

        public SenderCoroutine(Context context, String timerPrefix, SendGuideline guideline, ForwardInformation forwardInfo, Object msg,
                String id) {
            this.context = context;
            this.timerPrefix = timerPrefix;
            this.guideline = guideline;
            this.forwardInfo = forwardInfo;
            this.msg = msg;
            this.id = id;
        }

        @Override
        public void run(Continuation cnt) throws Exception {
            // Fire off message
            context.addOutgoingMessage(
                    forwardInfo.getProxyFromId(),
                    forwardInfo.getProxyToAddress(),
                    msg);

            // Resend
            for (Duration duration : guideline.getSendDurations()) {
                // Ask timer to trigger us again after duration
                String timerAddress = timerPrefix + SEPARATOR + duration.toMillis();
                context.addOutgoingMessage(id, timerAddress, EXECUTE_OUTBOUND_TIMER_MSG);
                cnt.suspend();

                // Fire off message
                context.addOutgoingMessage(
                        forwardInfo.getProxyFromId(),
                        forwardInfo.getProxyToAddress(),
                        msg);
            }

            // Final wait before discard
            Duration finalWaitDuration = guideline.getAckWaitDuration();
            String timerAddress = timerPrefix + SEPARATOR + finalWaitDuration.toMillis();
            context.addOutgoingMessage(id, timerAddress, EXECUTE_OUTBOUND_TIMER_MSG);
            cnt.suspend();
        }

    }

    private static final class OutboundRequestState {

        private final String requesterAddress;
        private final CoroutineRunner coroutineRunner;
        private boolean responseArrived;

        public OutboundRequestState(String requesterAddress, CoroutineRunner coroutineRunner) {
            Validate.notNull(requesterAddress);
            Validate.notNull(coroutineRunner);
            this.requesterAddress = requesterAddress;
            this.coroutineRunner = coroutineRunner;
        }

        public String getRequesterAddress() {
            return requesterAddress;
        }

        public CoroutineRunner getCoroutineRunner() {
            return coroutineRunner;
        }

        public boolean isResponseArrived() {
            return responseArrived;
        }

        public void setResponseArrived(boolean responseArrived) {
            this.responseArrived = responseArrived;
        }

    }

    private static final class InboundRequestState {

        private final String sourceAddress;
        private Object response;

        public InboundRequestState(String sourceAddress) {
            Validate.notNull(sourceAddress);
            this.sourceAddress = sourceAddress;
        }

        public String getSourceAddress() {
            return sourceAddress;
        }

        public Object getResponse() {
            return response;
        }

        public void setResponse(Object response) {
            Validate.notNull(response);
            this.response = response;
        }

    }
}
