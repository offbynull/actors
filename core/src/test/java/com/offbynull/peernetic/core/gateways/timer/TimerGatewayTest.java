package com.offbynull.peernetic.core.gateways.timer;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import java.util.concurrent.ArrayBlockingQueue;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class TimerGatewayTest {

    @Test
    public void mustEchoBackMessageAfter2Seconds() throws Exception {
        ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<>(3);

        Coroutine tester = (cnt) -> {
            Context ctx = (Context) cnt.getContext();

            String timerPrefix = ctx.getIncomingMessage();
            ctx.addOutgoingMessage("fromid", timerPrefix + ":2000:extra", "msg");
            cnt.suspend();
            
            queue.add(ctx.getSource());
            queue.add(ctx.getDestination());
            queue.add(ctx.getIncomingMessage());
        };

        TimerGateway timerGateway = new TimerGateway("timer");
        Shuttle timerInputShuttle = timerGateway.getIncomingShuttle();

        ActorThread testerThread = ActorThread.create("local");
        Shuttle testerInputShuttle = testerThread.getIncomingShuttle();

        testerThread.addOutgoingShuttle(timerInputShuttle);
        timerGateway.addOutgoingShuttle(testerInputShuttle);

        testerThread.addCoroutineActor("tester", tester, "timer");

        assertEquals("timer:2000:extra", queue.take());
        assertEquals("local:tester:fromid", queue.take());
        assertEquals("msg", queue.take());
        
        testerThread.close();
        testerThread.join();
    }
    
}
