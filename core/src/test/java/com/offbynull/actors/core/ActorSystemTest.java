package com.offbynull.actors.core;

import com.offbynull.actors.core.actor.Context;
import com.offbynull.coroutines.user.Coroutine;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;
import static org.junit.Assert.*;

public class ActorSystemTest {

    @Test(timeout = 2000L)
    public void mustCreateAndCommunicateBetweenActorsAndDefaultTimer() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Coroutine echoer = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.ruleSet().allowAll();

            // Wait for message to come in to echo and echo it back
            cnt.suspend();
            ctx.out("local:sender", ctx.in());
        };

        Coroutine sender = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.ruleSet().allowAll();
            
            // Pause for half a second to allow echoer actor to start up / initialize
            ctx.timer(500L, new Object());
            cnt.suspend();
            
            // Send a message to echoer
            ctx.out("local:echoer", "hi");
            cnt.suspend();

            // Ensure we got a repsonse echo'd back
            assertEquals(ctx.in(), "hi");

            latch.countDown();
        };

        try (ActorSystem actorSystem = ActorSystem.builder()
                .withRunnerCoreCount(1)
                .withRunnerName("local")
                .withLogGateway()
                .withTimerGateway()
                .withDirectGateway()
                .withActor("echoer", echoer, new Object())
                .withActor("sender", sender, new Object())
                .build()) {
            latch.await();
        }
    }
    
}
