package com.offbynull.peernetic.core.actors.reliableproxy;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.common.AddressUtils;
import static java.lang.Math.random;
import java.util.HashSet;
import java.util.Set;

public final class RetryReceiveProxyCoroutine implements Coroutine {

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        StartRetryReceiveProxy startProxy = ctx.getIncomingMessage();
        String timerAddressPrefix = startProxy.getTimerPrefix();
        String self = ctx.getSelf();

        Set<Long> cacheSet = new HashSet<>();

        while (true) {
            cnt.suspend();
            
            String src = ctx.getSource();
            
            CoroutineRunner coroutineRunner;
            long id;
            if (AddressUtils.isParent(timerAddressPrefix, src)) {// This is from the timer
                String idStr = AddressUtils.relativize(self, ctx.getDestination());
                id = Long.parseLong(idStr);
                cacheSet.remove(id);
            } else {
                Object msg = ctx.getIncomingMessage();
                
                id = src; // get ID from src
                
                ctx.addOutgoingMessage(id, src, "");
                cacheSet.add(id);
            }
        }
    }

}
