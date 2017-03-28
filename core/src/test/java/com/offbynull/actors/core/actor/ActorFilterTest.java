package com.offbynull.actors.core.actor;

import com.offbynull.actors.core.context.Context;
import com.offbynull.coroutines.user.Coroutine;
import static com.offbynull.actors.core.context.Context.ForwardMode.FORWARD_AND_FORGET;
import com.offbynull.actors.core.gateways.direct.DirectGateway;
import com.offbynull.actors.core.shuttle.Address;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

public class ActorFilterTest {
    private ActorRunner runner;
    private DirectGateway direct1;
    private DirectGateway direct2;

    @Before
    public void setUp() {
        runner = ActorRunner.create("runner", 1);
        direct1 = DirectGateway.create("direct1");
        direct2 = DirectGateway.create("direct2");
        
        direct1.addOutgoingShuttle(runner.getIncomingShuttle());
        runner.addOutgoingShuttle(direct1.getIncomingShuttle());
        
        direct2.addOutgoingShuttle(runner.getIncomingShuttle());
        runner.addOutgoingShuttle(direct2.getIncomingShuttle());
    }

    @After
    public void tearDown() throws Exception {
        runner.close();
        direct1.close();
        direct2.close();
    }
    
    @Test
    public void mustBlockByDefault() throws Exception {
        Coroutine level0 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.intercept(true);
            
            ctx.out("direct1", "ready");

            while (true) {
                cnt.suspend();
                if (ctx.destination().equals(ctx.self())) {
                    ctx.out(ctx.source(), ctx.in());
                }
            }
        };
        
        runner.addActor("level0", level0, new Object());
        
        
        assertEquals("ready", direct1.readMessagePayloadOnly());
        direct1.writeMessage("runner:level0", "echomsg");
        assertNull(direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
    }

    @Test
    public void mustAlwaysBlock() throws Exception {
        Coroutine level0 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.intercept(true);
            ctx.block();
            
            ctx.out("direct1", "ready");

            while (true) {
                cnt.suspend();
                if (ctx.destination().equals(ctx.self())) {
                    ctx.out(ctx.source(), ctx.in());
                }
            }
        };
        
        runner.addActor("level0", level0, new Object());
        
        
        assertEquals("ready", direct1.readMessagePayloadOnly());
        direct1.writeMessage("runner:level0", "echomsg");
        assertNull(direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
    }

    @Test
    public void mustBlockExceptForAllowedAddress() throws Exception {
        Coroutine level0 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.intercept(true);
            ctx.block();
            ctx.allow("direct1", false);
            
            ctx.out("direct1", "ready");

            while (true) {
                cnt.suspend();
                if (Address.fromString("direct1").equals(ctx.source())) {
                    ctx.out(ctx.source(), ctx.in());
                }
            }
        };
        
        runner.addActor("level0", level0, new Object());
        
        
        assertEquals("ready", direct1.readMessagePayloadOnly());
        direct1.writeMessage("runner:level0", "echomsg");
        assertEquals("echomsg", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
    }

    @Test
    public void mustBlockChildren() throws Exception { // direct is accept but children of direct are rejected
        Coroutine level1 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            
            ctx.out("direct1", "ready");

            while (true) {
                cnt.suspend();
                if (ctx.source().getElement(0).equals("direct1")) {
                    ctx.out(ctx.source(), ctx.in() + "1");
                }
            }
        };

        Coroutine level0 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.intercept(true);
            
            ctx.block();
            ctx.allow(ctx.self(), true);
            ctx.allow("direct1", false);
            
            ctx.child("level1", level1, new Object());
            
            ctx.out("direct1", "ready");

            while (true) {
                cnt.suspend();
                ctx.forward(FORWARD_AND_FORGET);
                if (ctx.source().getElement(0).equals("direct1")) {
                    ctx.out(ctx.source(), ctx.in() + "0");
                }
            }
        };
        
        runner.addActor("level0", level0, new Object());
        
        
        assertEquals("ready", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
        assertEquals("ready", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
        
        direct1.writeMessage("direct1", "runner:level0:level1", "echomsg");
        assertEquals("echomsg0", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
        assertEquals("echomsg1", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));

        direct1.writeMessage("direct1:child", "runner:level0:level1", "echomsg");
        assertNull("echomsg0", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
        assertNull("echomsg1", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
    }

    @Test
    public void mustBlockExceptForAllowedAddressAndItsChildren() throws Exception {
        Coroutine level1 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            
            ctx.out("direct1", "ready");

            while (true) {
                cnt.suspend();
                if (ctx.source().getElement(0).equals("direct1")) {
                    ctx.out(ctx.source(), ctx.in() + "1");
                }
            }
        };

        Coroutine level0 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.intercept(true);
            
            ctx.block();
            ctx.allow(ctx.self(), true);
            ctx.allow("direct1", true);
            
            ctx.child("level1", level1, new Object());
            
            ctx.out("direct1", "ready");

            while (true) {
                cnt.suspend();
                ctx.forward(FORWARD_AND_FORGET);
                if (ctx.source().getElement(0).equals("direct1")) {
                    ctx.out(ctx.source(), ctx.in() + "0");
                }
            }
        };
        
        runner.addActor("level0", level0, new Object());
        
        
        assertEquals("ready", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
        assertEquals("ready", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
        
        direct1.writeMessage("direct1", "runner:level0:level1", "echomsg");
        assertEquals("echomsg0", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
        assertEquals("echomsg1", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
        
        direct1.writeMessage("direct1:child", "runner:level0:level1", "echomsg");
        assertEquals("echomsg0", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
        assertEquals("echomsg1", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
    }

    @Test
    public void mustBlockExceptForAllowedAddressAndItsChildrenButNotOneSpecificChild() throws Exception {
        Coroutine level3 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            
            ctx.out("direct1", "ready");

            while (true) {
                cnt.suspend();
                if (ctx.source().getElement(0).equals("direct1")) {
                    ctx.out(ctx.source(), ctx.in() + "3");
                }
            }
        };

        Coroutine level2 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            
            ctx.out("direct1", "ready");
            ctx.child("level3", level3, new Object()); // prime msg will never reach level3 because it's blocked at level0

            while (true) {
                cnt.suspend();
                if (ctx.source().getElement(0).equals("direct1")) {
                    ctx.out(ctx.source(), ctx.in() + "2");
                }
            }
        };

        Coroutine level1 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            
            ctx.out("direct1", "ready");
            ctx.child("level2", level2, new Object());

            while (true) {
                cnt.suspend();
                if (ctx.source().getElement(0).equals("direct1")) {
                    ctx.out(ctx.source(), ctx.in() + "1");
                }
            }
        };

        Coroutine level0 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.intercept(true);
            
            ctx.block();
            ctx.allow("runner:level0", true);
            ctx.block("runner:level0:level1:level2:level3", false);
            ctx.allow("direct1", false);
            
            ctx.child("level1", level1, new Object());
            
            ctx.out("direct1", "ready");

            while (true) {
                cnt.suspend();
                ctx.forward(FORWARD_AND_FORGET);
                if (ctx.source().getElement(0).equals("direct1")) {
                    ctx.out(ctx.source(), ctx.in() + "0");
                }
            }
        };
        
        runner.addActor("level0", level0, new Object());
        
        
        assertEquals("ready", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
        assertEquals("ready", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
        assertEquals("ready", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
        assertNull(direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));

        direct1.writeMessage("direct1", "runner:level0", "echomsg");
        assertEquals("echomsg0", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));

        direct1.writeMessage("direct1", "runner:level0:level1", "echomsg");
        assertEquals("echomsg0", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
        assertEquals("echomsg1", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));

        direct1.writeMessage("direct1", "runner:level0:level1:level2", "echomsg");
        assertEquals("echomsg0", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
        assertEquals("echomsg2", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));

        direct1.writeMessage("direct1", "runner:level0:level1:level2:level3", "echomsg");
        assertEquals("echomsg0", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
        assertNull(direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS)); // never get back echomsg3 because that child is blocked
    }

    
    
    
    
    
    @Test
    public void mustAlwaysAllow() throws Exception {
        Coroutine level0 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.intercept(true);
            ctx.allow();
            
            ctx.out("direct1", "ready");

            while (true) {
                cnt.suspend();
                if (ctx.destination().equals(ctx.self())) {
                    ctx.out(ctx.source(), ctx.in());
                }
            }
        };
        
        runner.addActor("level0", level0, new Object());
        
        
        assertEquals("ready", direct1.readMessagePayloadOnly());
        direct1.writeMessage("runner:level0", "echomsg");
        assertEquals("echomsg", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
    }
    
    @Test
    public void mustAllowExceptForBlockedAddress() throws Exception {
        Coroutine level0 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.intercept(true);
            ctx.allow();
            ctx.block("direct2", false);
            
            ctx.out("direct1", "ready");
            ctx.out("direct2", "ready");

            while (true) {
                cnt.suspend();
                if (ctx.source().getElement(0).startsWith("direct")) {
                    ctx.out(ctx.source(), ctx.in());
                }
            }
        };
        
        runner.addActor("level0", level0, new Object());
        
        
        assertEquals("ready", direct1.readMessagePayloadOnly());
        assertEquals("ready", direct2.readMessagePayloadOnly());
        
        direct1.writeMessage("runner:level0", "echomsg");
        assertEquals("echomsg", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
        
        direct2.writeMessage("runner:level0", "echomsg");
        assertNull(direct2.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
        
        direct2.writeMessage("direct2:child", "runner:level0", "echomsg");    // must work for children
        assertEquals("echomsg", direct2.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
    }

    @Test
    public void mustAllowExceptForBlockedAddressAndItsChildren() throws Exception {
        Coroutine level0 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.intercept(true);
            ctx.allow();
            ctx.block("direct2", true);
            
            ctx.out("direct1", "ready");
            ctx.out("direct2", "ready");

            while (true) {
                cnt.suspend();
                if (ctx.source().getElement(0).startsWith("direct")) {
                    ctx.out(ctx.source(), ctx.in());
                }
            }
        };
        
        runner.addActor("level0", level0, new Object());
        
        
        assertEquals("ready", direct1.readMessagePayloadOnly());
        assertEquals("ready", direct2.readMessagePayloadOnly());
        
        direct1.writeMessage("runner:level0", "echomsg");
        assertEquals("echomsg", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
        
        direct2.writeMessage("runner:level0", "echomsg");
        assertNull(direct2.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
        
        direct2.writeMessage("direct2:child", "runner:level0", "echomsg");
        assertNull(direct2.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
    }

    
    
    
    
    

    @Test
    public void mustBeAbleToSwitchAccessOfAnAddress() throws Exception {
        Coroutine level0 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.intercept(true);
            ctx.allow();
            
            ctx.out("direct1", "ready");
            ctx.out("direct2", "ready");
            
            ctx.block("direct2", true);
            cnt.suspend();
            if (ctx.source().getElement(0).startsWith("direct")) {
                ctx.out(ctx.source(), ctx.in());
            }
            
            ctx.allow("direct2", true);
            cnt.suspend();
            if (ctx.source().getElement(0).startsWith("direct")) {
                ctx.out(ctx.source(), ctx.in());
            }
        };
        
        runner.addActor("level0", level0, new Object());
        
        
        assertEquals("ready", direct1.readMessagePayloadOnly());
        assertEquals("ready", direct2.readMessagePayloadOnly());

        direct2.writeMessage("runner:level0", "echomsg");
        assertNull(direct2.readMessagePayloadOnly(1L, TimeUnit.SECONDS));

        direct1.writeMessage("runner:level0", "echomsg");
        assertEquals("echomsg", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));

        direct2.writeMessage("runner:level0", "echomsg");
        assertEquals("echomsg", direct2.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
    }

    
    
    
    
    
    @Test
    public void mustAllowOnlyCertainTypes() throws Exception {
        Coroutine level0 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            
            ctx.block();
            ctx.allow("direct1", true, String.class);
            
            ctx.out("direct1", "ready");
            
            while (true) {
                cnt.suspend();
                if (ctx.source().getElement(0).startsWith("direct")) {
                    ctx.out(ctx.source(), ctx.in());
                }
            }
        };
        
        runner.addActor("level0", level0, new Object());
        
        
        assertEquals("ready", direct1.readMessagePayloadOnly());

        direct1.writeMessage("runner:level0", "echomsg");
        assertEquals("echomsg", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));

        direct1.writeMessage("runner:level0", 1);
        assertNull(direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));

        direct1.writeMessage("runner:level0", "echomsg");
        assertEquals("echomsg", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
    }

    @Test
    public void mustAllowOnlyExactTypesNotInheritedTypes() throws Exception {
        Coroutine level0 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            
            ctx.block();
            ctx.allow("direct1", true, Object.class);
            
            ctx.out("direct1", "ready");
            
            while (true) {
                cnt.suspend();
                if (ctx.source().getElement(0).startsWith("direct")) {
                    ctx.out(ctx.source(), ctx.in());
                }
            }
        };
        
        runner.addActor("level0", level0, new Object());
        
        
        assertEquals("ready", direct1.readMessagePayloadOnly());

        Object obj = new Object();
        
        direct1.writeMessage("runner:level0", obj);
        assertEquals(obj, direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));

        direct1.writeMessage("runner:level0", 1);
        assertNull(direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));

        direct1.writeMessage("runner:level0", obj);
        assertEquals(obj, direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
    }

    
    
    
    
    
    @Test
    public void mustBlockOnlyCertainTypes() throws Exception {
        Coroutine level0 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            
            ctx.allow();
            ctx.block("direct1", true, Integer.class);
            
            ctx.out("direct1", "ready");
            
            while (true) {
                cnt.suspend();
                if (ctx.source().getElement(0).startsWith("direct")) {
                    ctx.out(ctx.source(), ctx.in());
                }
            }
        };
        
        runner.addActor("level0", level0, new Object());
        
        
        assertEquals("ready", direct1.readMessagePayloadOnly());

        direct1.writeMessage("runner:level0", "echomsg");
        assertEquals("echomsg", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));

        direct1.writeMessage("runner:level0", 1);
        assertNull(direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));

        direct1.writeMessage("runner:level0", "echomsg");
        assertEquals("echomsg", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
    }
    
    @Test
    public void mustBlockExactTypesNotInheritedTypes() throws Exception {
        Coroutine level0 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            
            ctx.allow();
            ctx.block("direct1", true, Object.class);
            
            ctx.out("direct1", "ready");
            
            while (true) {
                cnt.suspend();
                if (ctx.source().getElement(0).startsWith("direct")) {
                    ctx.out(ctx.source(), ctx.in());
                }
            }
        };
        
        runner.addActor("level0", level0, new Object());
        
        
        assertEquals("ready", direct1.readMessagePayloadOnly());

        Object obj = new Object();
        
        direct1.writeMessage("runner:level0", obj);
        assertNull(direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));

        direct1.writeMessage("runner:level0", "echomsg");
        assertEquals("echomsg", direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));

        direct1.writeMessage("runner:level0", obj);
        assertNull(direct1.readMessagePayloadOnly(1L, TimeUnit.SECONDS));
    }
}
