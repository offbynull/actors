package com.offbynull.peernetic.core.actors.retry;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.common.AddressUtils;
import static com.offbynull.peernetic.core.common.AddressUtils.SEPARATOR;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.Validate;

public final class RetrySendProxyCoroutine implements Coroutine {

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        StartRetrySendProxy startProxy = ctx.getIncomingMessage();
        IdExtractor idExtractor = startProxy.getIdExtractor();
        String timerAddressPrefix = startProxy.getTimerPrefix();
        String dstAddress = startProxy.getDestinationAddress();
        String self = ctx.getSelf();

        Map<String, MessageState> cache = new HashMap<>();

        while (true) {
            cnt.suspend();

            String from = ctx.getSource();

            if (AddressUtils.isParent(timerAddressPrefix, from)) { // timer
                // Timer indicated that a coroutine needs to be run
                String id = AddressUtils.relativize(self, ctx.getDestination());
                MessageState msgState = cache.get(id);
                if (msgState != null) {
                    CoroutineRunner sender = msgState.getCoroutineRunner();
                    boolean stillRunning = sender.execute();
                    if (!stillRunning) {
                        cache.remove(id);
                    }
                }
            } else if (AddressUtils.isParent(dstAddress, from)) { // incoming resp
                // Response has come in, find out who response was for
                Object msg = ctx.getIncomingMessage();
                String id = idExtractor.getId(msg);
                
                MessageState msgState = cache.get(id);
                
                // If we can't find out who response was for, ignore
                if (msgState == null) {
                    continue;
                }
                
                // If the message already has a response, ignore
                if (msgState.isResponseArrived()) {
                    continue;
                }
                
                // Send response and save
                String to = msgState.getRequesterAddress();
                ctx.addOutgoingMessage(to, msg);
            } else {
                // Request has come in. Make sure id isn't one we've already cached
                Object msg = ctx.getIncomingMessage();
                String id = idExtractor.getId(msg);

                SenderCoroutine senderCoroutine = new SenderCoroutine(ctx, startProxy, msg, id);
                CoroutineRunner sender = new CoroutineRunner(senderCoroutine);
                MessageState msgState = new MessageState(from, sender);
                Validate.isTrue(!cache.containsKey(id));
                
                // Cache request
                cache.put(id, msgState);

                // Send request
                boolean stillRunning = sender.execute();
                if (!stillRunning) {
                    cache.remove(id);
                }
            } 
        }
    }

    private static final class SenderCoroutine implements Coroutine {

        private Context context;
        private StartRetrySendProxy startProxy;
        private Object msg;
        private String id;

        public SenderCoroutine(Context context, StartRetrySendProxy startProxy, Object msg, String id) {
            this.context = context;
            this.startProxy = startProxy;
            this.msg = msg;
            this.id = id;
        }

        @Override
        public void run(Continuation cnt) throws Exception {
            String dstPrefix = startProxy.getDestinationAddress();
            String timerPrefix = startProxy.getTimerPrefix();
            
            SendGuidelineGenerator generator = startProxy.getGenerator();
            SendGuideline guideline = generator.generate(msg);
            
            String self = context.getSelf();
            String fullSelf = context.getDestination();
            String suffix = AddressUtils.relativize(self, fullSelf);
            
            String dstAddress = dstPrefix;
            if (!suffix.isEmpty()) {
                dstAddress += SEPARATOR + suffix;
            }

            
            // Fire off message
            context.addOutgoingMessage(dstAddress, msg);

            // Resend
            for (Duration duration : guideline.getSendDurations()) {
                // Ask timer to trigger us again after duration
                String timerAddress = timerPrefix + SEPARATOR + duration.toMillis();
                context.addOutgoingMessage(id, timerAddress, "");
                cnt.suspend();

                // Fire off message
                context.addOutgoingMessage(dstAddress, msg);
            }

            // Final wait before discard
            Duration finalWaitDuration = guideline.getAckWaitDuration();
            String timerAddress = timerPrefix + SEPARATOR + finalWaitDuration.toMillis();
            context.addOutgoingMessage(id, timerAddress, "");
            cnt.suspend();
        }

    }

    private static final class MessageState {

        private final String requesterAddress;
        private final CoroutineRunner coroutineRunner;
        private boolean responseArrived;

        public MessageState(String requesterAddress, CoroutineRunner coroutineRunner) {
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
}
