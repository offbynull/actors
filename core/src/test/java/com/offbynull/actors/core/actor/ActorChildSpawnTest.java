package com.offbynull.actors.core.actor;

import com.offbynull.actors.core.context.Context;
import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import static com.offbynull.actors.core.context.Context.ForwardMode.FORWARD_AND_FORGET;
import static com.offbynull.actors.core.context.Context.ForwardMode.FORWARD_AND_RETURN;
import com.offbynull.actors.core.gateways.direct.DirectGateway;
import com.offbynull.actors.core.shuttle.Address;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.Before;

public class ActorChildSpawnTest {
    
    private ActorRunner runner;
    private DirectGateway direct;

    @Before
    public void setUp() {
        runner = ActorRunner.create("runner", 1);
        direct = DirectGateway.create("direct");
        
        direct.addOutgoingShuttle(runner.getIncomingShuttle());
        runner.addOutgoingShuttle(direct.getIncomingShuttle());
    }

    @After
    public void tearDown() throws Exception {
        runner.close();
        direct.close();
    }
    
    @Test(timeout = 2000L)
    public void mustCommunicateWithTheCorrectChildActor() throws Exception {
        Coroutine level1_0 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            ctx.out("direct", "ready");

            while (true) {
                cnt.suspend();
                if (Address.fromString("direct").equals(ctx.source())) {
                    ctx.out("direct", ctx.in());
                }
            }
        };
        
        Coroutine level1_1 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            ctx.out("direct", "ready");

            while (true) {
                cnt.suspend();
                if (Address.fromString("direct").equals(ctx.source())) {
                    ctx.out("direct", "BAD CHILD CALLED");
                }
            }
        };
        
        Coroutine level0 = (Continuation cnt) -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            ctx.child("level1_0", level1_0, new Object());
            ctx.child("level1_1", level1_1, new Object());

            while (true) {
                ctx.forward(FORWARD_AND_FORGET);
                cnt.suspend();
            }
        };
        
        runner.addActor("level0", level0, new Object());
        
        
        assertEquals("ready", direct.readMessagePayloadOnly());
        assertEquals("ready", direct.readMessagePayloadOnly());
        direct.writeMessage("runner:level0:level1_0", "hi!");
        assertEquals("hi!", direct.readMessagePayloadOnly());
    }
    
    @Test(timeout = 2000L)
    public void mustInterceptButNotForward() throws Exception {
        Coroutine level1 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();

            ctx.out("direct", "child ready!");
            while (true) {
                ctx.forward(FORWARD_AND_FORGET);
                cnt.suspend();
                if (Address.fromString("direct").equals(ctx.source())) {
                    ctx.out(ctx.source(), "hello from child!");
                }
            }
        };
        
        Coroutine level0 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            ctx.intercept(true);

            ctx.out("direct", "parent ready!");
            ctx.child("level1", level1, new Object());

            while (true) {
                ctx.forward(FORWARD_AND_FORGET);
                cnt.suspend();
                if (Address.fromString("direct").equals(ctx.source())) {
                    ctx.out(ctx.source(), "hello from main!");
                }
            }
        };
        
        runner.addActor("level0", level0, new Object());
        


        assertEquals("parent ready!", direct.readMessagePayloadOnly());
        assertEquals("child ready!", direct.readMessagePayloadOnly());
        direct.writeMessage("runner:level0:level1", "hi!");
        assertEquals("hello from main!", direct.readMessagePayloadOnly());
    }
    
    @Test(timeout = 2000L)
    public void mustInterceptAndForward() throws Exception {
        Coroutine level1 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();

            ctx.out("direct", "child ready!");
            while (true) {
                ctx.forward(FORWARD_AND_FORGET);
                cnt.suspend();
                if (Address.fromString("direct").equals(ctx.source())) {
                    ctx.out(ctx.source(), "hello from child!");
                }
            }
        };
        
        Coroutine level0 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            ctx.intercept(true);

            ctx.out("direct", "parent ready!");
            ctx.child("level1", level1, new Object());

            while (true) {
                cnt.suspend();
                if (Address.fromString("direct").equals(ctx.source())) {
                    ctx.forward(FORWARD_AND_RETURN);
                    ctx.out(ctx.source(), "pre-hello from main!");
                    cnt.suspend();
                    ctx.out(ctx.source(), "post-hello from main!");
                } else {
                    ctx.forward(FORWARD_AND_FORGET);
                }
            }
        };
        
        runner.addActor("level0", level0, new Object());
        


        assertEquals("parent ready!", direct.readMessagePayloadOnly());
        assertEquals("child ready!", direct.readMessagePayloadOnly());

        direct.writeMessage("runner:level0:level1", "hi!");
        assertEquals("pre-hello from main!", direct.readMessagePayloadOnly());
        assertEquals("hello from child!", direct.readMessagePayloadOnly());
        assertEquals("post-hello from main!", direct.readMessagePayloadOnly());
    }
}
