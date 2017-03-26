package com.offbynull.actors.core.gateways.timer;

import com.offbynull.actors.core.gateways.timer.TimerGateway;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.actors.core.actor.ActorRunner;
import com.offbynull.actors.core.actor.Context;
import com.offbynull.actors.core.shuttle.Shuttle;
import java.util.concurrent.ArrayBlockingQueue;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class TimerGatewayTest {

    @Test
    public void mustEchoBackMessageAfter2Seconds() throws Exception {
        ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<>(3);

        Coroutine tester = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();

            String timerPrefix = ctx.in();
            ctx.out("local:tester", "timer:2000:extra", "msg");
            cnt.suspend();
            
            queue.add(ctx.source().toString());
            queue.add(ctx.destination().toString());
            queue.add(ctx.in());
        };

        TimerGateway timerGateway = TimerGateway.create("timer");
        Shuttle timerInputShuttle = timerGateway.getIncomingShuttle();

        ActorRunner testerRunner = ActorRunner.create("local", 1);
        Shuttle testerInputShuttle = testerRunner.getIncomingShuttle();

        testerRunner.addOutgoingShuttle(timerInputShuttle);
        timerGateway.addOutgoingShuttle(testerInputShuttle);

        testerRunner.addActor("tester", tester, "timer");

        assertEquals("timer:2000:extra", queue.take());
        assertEquals("local:tester", queue.take());
        assertEquals("msg", queue.take());
        
        testerRunner.close();
        testerRunner.join();
    }
    
}
