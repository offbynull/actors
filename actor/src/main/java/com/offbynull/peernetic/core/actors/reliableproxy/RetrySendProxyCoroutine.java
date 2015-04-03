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
import java.util.Random;

public final class RetrySendProxyCoroutine implements Coroutine {

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        StartRetrySendProxy startProxy = ctx.getIncomingMessage();
        Random random = startProxy.getRandom();
        String timerAddressPrefix = startProxy.getTimerPrefix();
        String reqAddressPrefix = startProxy.getSourcePrefix();
        String respAddressPrefix = startProxy.getDestinationPrefix();
        String self = ctx.getSelf();

        Map<Long, CoroutineRunner> senders = new HashMap<>();

        while (true) {
            cnt.suspend();

            String from = ctx.getSource();

            if (AddressUtils.isParent(timerAddressPrefix, from)) { // timer
                // Get id of transmitter
                String idStr = AddressUtils.relativize(self, ctx.getDestination());
                long id = Long.parseLong(idStr);

                // Execute transmitter with that id (if it exists)
                CoroutineRunner transmitter = senders.get(id);
                if (transmitter != null) {
                    boolean stillRunning = transmitter.execute();
                    if (!stillRunning) {
                        senders.remove(id);
                    }
                }
            } else if (AddressUtils.isParent(respAddressPrefix, from)) { // incoming resp (response from destination)
                // Get id of transmitter
                String suffix = AddressUtils.relativize(respAddressPrefix, from);
                String idStr = AddressUtils.getLastAddressElement(suffix);
                long id = Long.parseLong(idStr);
                
                // Remove transmitter and send response to source (if transmitter existed)
                CoroutineRunner sender = senders.remove(id);
                if (sender != null) {
                    Object msg = ctx.getIncomingMessage();
                    ctx.addOutgoingMessage(
                            reqAddressPrefix + SEPARATOR + suffix,
                            msg);
                }
                
            } else if (AddressUtils.isParent(reqAddressPrefix, from)) { // outgoing msg (request from source)
                // Generate an id for this message (make sure id doesn't collide)
                long id = random.nextLong();
                while (senders.containsKey(id)) {
                    id++;
                }

                // Create and save transmitter
                Object msg = ctx.getIncomingMessage();
                SenderCoroutine senderCoroutine = new SenderCoroutine(ctx, startProxy, msg, id);
                CoroutineRunner sender = new CoroutineRunner(senderCoroutine);
                senders.put(id, sender);

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
        private long id;

        public SenderCoroutine(Context context, StartRetrySendProxy startProxy, Object msg, long id) {
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
            
            String idStr = Long.toString(id);
            String suffix = AddressUtils.relativize(srcPrefix, context.getSource()) + SEPARATOR + idStr;
            
            String dstAddress = dstPrefix + SEPARATOR + suffix;
            
            SendGuidelineGenerator generator = startProxy.getGenerator();
            SendGuideline guideline = generator.generate(msg);

            
            // Fire off message
            context.addOutgoingMessage(dstAddress, msg);

            // Resend
            for (Duration duration : guideline.getSendDurations()) {
                // Ask timer to trigger us again after duration
                String timerAddress = timerPrefix + SEPARATOR + duration.toMillis();
                context.addOutgoingMessage(idStr, timerAddress, "");
                cnt.suspend();

                // Fire off message
                context.addOutgoingMessage(dstAddress, msg);
            }

            // Final wait before discard
            Duration finalWaitDuration = guideline.getAckWaitDuration();
            String timerAddress = timerPrefix + SEPARATOR + finalWaitDuration.toMillis();
            context.addOutgoingMessage(idStr, timerAddress, "");
            cnt.suspend();
        }

    }

}
