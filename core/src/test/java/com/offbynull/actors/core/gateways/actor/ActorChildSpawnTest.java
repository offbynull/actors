package com.offbynull.actors.core.gateways.actor;

import static com.offbynull.actors.core.gateways.actor.Context.SuspendFlag.FORWARD_AND_RELEASE;
import static com.offbynull.actors.core.gateways.actor.Context.SuspendFlag.FORWARD_AND_RETURN;
import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.actors.core.gateways.direct.DirectGateway;
import com.offbynull.actors.core.shuttle.Address;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.Before;

public class ActorChildSpawnTest {
    
    private ActorGateway actor;
    private DirectGateway direct;

    @Before
    public void setUp() {
        actor = ActorGateway.create("actor", 1);
        direct = DirectGateway.create("direct");
        
        direct.addOutgoingShuttle(actor.getIncomingShuttle());
        actor.addOutgoingShuttle(direct.getIncomingShuttle());
        
        direct.listen("direct");
    }

    @After
    public void tearDown() throws Exception {
        actor.close();
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
                ctx.mode(FORWARD_AND_RELEASE);
                cnt.suspend();
            }
        };
        
        actor.addActor("level0", level0, new Object());
        
        
        assertEquals("ready", direct.readMessagePayloadOnly("direct"));
        assertEquals("ready", direct.readMessagePayloadOnly("direct"));
        direct.writeMessage("actor:level0:level1_0", "hi!");
        assertEquals("hi!", direct.readMessagePayloadOnly("direct"));
    }
    
    @Test(timeout = 2000L)
    public void mustInterceptButNotForward() throws Exception {
        Coroutine level1 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();

            ctx.out("direct", "child ready!");
            while (true) {
                ctx.mode(FORWARD_AND_RELEASE);
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
                ctx.mode(FORWARD_AND_RELEASE);
                cnt.suspend();
                if (Address.fromString("direct").equals(ctx.source())) {
                    ctx.out(ctx.source(), "hello from main!");
                }
            }
        };
        
        actor.addActor("level0", level0, new Object());
        


        assertEquals("parent ready!", direct.readMessagePayloadOnly("direct"));
        assertEquals("child ready!", direct.readMessagePayloadOnly("direct"));
        direct.writeMessage("actor:level0:level1", "hi!");
        assertEquals("hello from main!", direct.readMessagePayloadOnly("direct"));
    }
    
    @Test(timeout = 2000L)
    public void mustInterceptAndForward() throws Exception {
        Coroutine level1 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();

            ctx.out("direct", "child ready!");
            while (true) {
                ctx.mode(FORWARD_AND_RELEASE);
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
                    ctx.mode(FORWARD_AND_RETURN);
                    ctx.out(ctx.source(), "pre-hello from main!");
                    cnt.suspend();
                    ctx.out(ctx.source(), "post-hello from main!");
                } else {
                    ctx.mode(FORWARD_AND_RELEASE);
                }
            }
        };
        
        actor.addActor("level0", level0, new Object());
        


        assertEquals("parent ready!", direct.readMessagePayloadOnly("direct"));
        assertEquals("child ready!", direct.readMessagePayloadOnly("direct"));

        direct.writeMessage("actor:level0:level1", "hi!");
        assertEquals("pre-hello from main!", direct.readMessagePayloadOnly("direct"));
        assertEquals("hello from child!", direct.readMessagePayloadOnly("direct"));
        assertEquals("post-hello from main!", direct.readMessagePayloadOnly("direct"));
    }
}
