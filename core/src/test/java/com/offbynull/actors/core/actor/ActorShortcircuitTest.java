
package com.offbynull.actors.core.actor;

import com.offbynull.actors.core.context.Context;
import static com.offbynull.actors.core.context.Context.ShortcircuitAction.PASS;
import static com.offbynull.actors.core.context.Context.ShortcircuitAction.PROCESS;
import static com.offbynull.actors.core.context.Context.ShortcircuitAction.TERMINATE;
import com.offbynull.actors.core.gateways.direct.DirectGateway;
import com.offbynull.coroutines.user.Coroutine;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.junit.Before;

public class ActorShortcircuitTest {
    
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
    public void mustShortcircuitPassOnIntegerTypes() throws Exception {
        Coroutine actor0 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            ctx.shortcircuit(Integer.class, _ctx -> {
                _ctx.out("saw integer");
                return PASS;
            });

            ctx.out("direct", "ready");
            
            while (true) {
                cnt.suspend();
                ctx.out("saw other");
            }
        };
        
        runner.addActor("actor0", actor0, new Object());
        
        
        assertEquals("ready", direct.readMessagePayloadOnly());
        
        direct.writeMessage("runner:actor0", 1);
        assertEquals("saw integer", direct.readMessagePayloadOnly());
        direct.writeMessage("runner:actor0", "hi!");
        assertEquals("saw other", direct.readMessagePayloadOnly());
    }
    
    @Test(timeout = 2000L)
    public void mustShortcircuitProcessOnIntegerTypes() throws Exception {
        Coroutine actor0 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            ctx.shortcircuit(Integer.class, _ctx -> {
                _ctx.out("saw integer");
                return PROCESS;
            });

            ctx.out("direct", "ready");
            
            while (true) {
                cnt.suspend();
                ctx.out("got something!");
            }
        };
        
        runner.addActor("actor0", actor0, new Object());
        
        
        assertEquals("ready", direct.readMessagePayloadOnly());
        
        direct.writeMessage("runner:actor0", 1);
        assertEquals("saw integer", direct.readMessagePayloadOnly());
        assertEquals("got something!", direct.readMessagePayloadOnly());
        direct.writeMessage("runner:actor0", "hi!");
        assertEquals("got something!", direct.readMessagePayloadOnly());
    }
    
    @Test(timeout = 2000L)
    public void mustShortcircuitTerminateOnIntegerTypes() throws Exception {
        Coroutine actor0 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            ctx.shortcircuit(Integer.class, _ctx -> {
                _ctx.out("saw integer");
                return TERMINATE;
            });

            ctx.out("direct", "ready");
            
            while (true) {
                cnt.suspend();
                ctx.out("got something!");
            }
        };
        
        runner.addActor("actor0", actor0, new Object());
        
        
        assertEquals("ready", direct.readMessagePayloadOnly());
        
        direct.writeMessage("runner:actor0", 1);
        assertEquals("saw integer", direct.readMessagePayloadOnly());
        direct.writeMessage("runner:actor0", "hi!");
        assertNull(direct.readMessagePayloadOnly(500, TimeUnit.MILLISECONDS));
    }
    
    @Test(timeout = 2000L)
    public void mustOverrideAndRemoveShortcircuit() throws Exception {
        Coroutine actor0 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            
            ctx.out("direct", "ready");
            ctx.shortcircuit(Integer.class, _ctx -> {
                _ctx.out("saw integer");
                return PROCESS;
            });
            
            cnt.suspend();
            ctx.out("got something!");
            ctx.shortcircuit(Integer.class, _ctx -> {
                _ctx.out("saw integer again");
                return PROCESS;
            });
            
            cnt.suspend();
            ctx.out("got something!");
            ctx.shortcircuit(Integer.class, null);
            
            cnt.suspend();
            ctx.out("got something!");
        };
        
        runner.addActor("actor0", actor0, new Object());
        
        
        assertEquals("ready", direct.readMessagePayloadOnly());
        
        direct.writeMessage("runner:actor0", 1);
        assertEquals("saw integer", direct.readMessagePayloadOnly());
        assertEquals("got something!", direct.readMessagePayloadOnly());
        direct.writeMessage("runner:actor0", 2);
        assertEquals("saw integer again", direct.readMessagePayloadOnly());
        assertEquals("got something!", direct.readMessagePayloadOnly());
        direct.writeMessage("runner:actor0", 4);
        assertEquals("got something!", direct.readMessagePayloadOnly());
        assertNull(direct.readMessagePayloadOnly(500, TimeUnit.MILLISECONDS));
    }
}
