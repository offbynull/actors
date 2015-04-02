package com.offbynull.peernetic.core.actors.reliableproxy;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.common.AddressUtils;
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
        String self = ctx.getSelf();

        Map<Long, CoroutineRunner> transmitters = new HashMap<>();

        while (true) {
            cnt.suspend();
            
            String src = ctx.getSource();
            
            CoroutineRunner coroutineRunner;
            long id;
            if (AddressUtils.isParent(timerAddressPrefix, src)) {// This is from the timer
                String idStr = AddressUtils.relativize(self, ctx.getDestination());
                id = Long.parseLong(idStr);
                coroutineRunner = transmitters.get(id);
            } else {
                Object msg = ctx.getIncomingMessage();
                
                id = random.nextLong();
                while (transmitters.containsKey(id)) {
                    id++;
                }
                TransmissionCoroutine transmissionCoroutine = new TransmissionCoroutine(ctx, startProxy, msg, id);
                coroutineRunner = new CoroutineRunner(transmissionCoroutine);
                transmitters.put(id, coroutineRunner);
            }
            
            if (coroutineRunner == null) {
                continue;
            }
            
            // execute transmission coroutine, if finished then remove
            if (!coroutineRunner.execute()) {
                transmitters.remove(id);
            }
        }
    }

    private static final class TransmissionCoroutine implements Coroutine {

        private Context context;
        private StartRetrySendProxy startProxy;
        private Object msg;
        private long id;

        public TransmissionCoroutine(Context context, StartRetrySendProxy startProxy, Object msg, long id) {
            this.context = context;
            this.startProxy = startProxy;
            this.msg = msg;
            this.id = id;
        }

        @Override
        public void run(Continuation cnt) throws Exception {
            String dstAddress = startProxy.getDestinationAddress();
            String timerAddressPrefix = startProxy.getTimerPrefix();
            
            SendGuidelineGenerator generator = startProxy.getGenerator();
            SendGuideline guideline = generator.generate(msg);
            
            String idStr = Long.toString(id);
            
            // Fire off message
            context.addOutgoingMessage(idStr, dstAddress, msg);

            // Resend
            for (Duration duration : guideline.getSendDurations()) {
                // Ask timer to trigger us again after duration
                String timerAddress = timerAddressPrefix + ":" + duration.toMillis();
                context.addOutgoingMessage(idStr, timerAddress, "");
                cnt.suspend();

                // Fire off message
                context.addOutgoingMessage(idStr, dstAddress, msg);
            }

            // Final wait before discard
            Duration finalWaitDuration = guideline.getAckWaitDuration();
            String timerAddress = timerAddressPrefix + ":" + finalWaitDuration.toMillis() + ":" + context.getSelf() + ":" + idStr;
            context.addOutgoingMessage(idStr, timerAddress, "");
            cnt.suspend();
        }

    }

}
