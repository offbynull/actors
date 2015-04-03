package com.offbynull.peernetic.core.actors.reliableproxy;

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
        String reqAddressPrefix = startProxy.getSourcePrefix();
        String respAddressPrefix = startProxy.getDestinationPrefix();
        String self = ctx.getSelf();

        Map<String, CoroutineRunner> senders = new HashMap<>();

        while (true) {
            cnt.suspend();

            String from = ctx.getSource();

            if (AddressUtils.isParent(timerAddressPrefix, from)) { // timer
                // Get id of transmitter
                String id = AddressUtils.relativize(self, ctx.getDestination());

                // Execute transmitter with that id (if it exists)
                CoroutineRunner transmitter = senders.get(id);
                if (transmitter != null) {
                    boolean stillRunning = transmitter.execute();
                    if (!stillRunning) {
                        senders.remove(id);
                    }
                }
            } else if (AddressUtils.isParent(respAddressPrefix, from)) { // incoming resp (response from destination)
                // Get id of response
                Object msg = ctx.getIncomingMessage();
                String id = idExtractor.getId(msg);
                
                String suffix = AddressUtils.relativize(respAddressPrefix, from);
                
                // Remove transmitter and send response to source (if transmitter existed)
                CoroutineRunner sender = senders.remove(id);
                if (sender != null) {
                    ctx.addOutgoingMessage(
                            reqAddressPrefix + SEPARATOR + suffix,
                            msg);
                }
                
            } else if (AddressUtils.isParent(reqAddressPrefix, from)) { // outgoing msg (request from source)
                // Create and save transmitter
                Object msg = ctx.getIncomingMessage();
                String id = idExtractor.getId(msg);
                
                SenderCoroutine senderCoroutine = new SenderCoroutine(ctx, startProxy, msg, id);
                CoroutineRunner sender = new CoroutineRunner(senderCoroutine);
                boolean alreadyExists = senders.putIfAbsent(id, sender) != null;
                
                Validate.isTrue(!alreadyExists); // must not be duplicating an existing id

                // Execute a transmitter cycle
                boolean stillRunning = sender.execute();
                if (!stillRunning) {
                    senders.remove(id);
                }
            } else {
                throw new IllegalStateException();
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
            String srcPrefix = startProxy.getSourcePrefix();
            String dstPrefix = startProxy.getDestinationPrefix();
            String timerPrefix = startProxy.getTimerPrefix();

            String suffix = AddressUtils.relativize(srcPrefix, context.getSource());
            String dstAddress = dstPrefix + SEPARATOR + suffix;
            
            SendGuidelineGenerator generator = startProxy.getGenerator();
            SendGuideline guideline = generator.generate(msg);

            
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

}
