
package com.offbynull.actors.gateways.actor;

import static com.offbynull.actors.gateways.actor.Context.ShortcircuitAction.PASS;
import static com.offbynull.actors.gateways.actor.Context.ShortcircuitAction.PROCESS;
import static com.offbynull.actors.gateways.actor.Context.ShortcircuitAction.TERMINATE;
import com.offbynull.actors.gateways.direct.DirectGateway;
import com.offbynull.coroutines.user.Coroutine;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.junit.Before;

public class ActorShortcircuitTest {
    
    private ActorGateway actor;
    private DirectGateway direct;

    @Before
    public void setUp() {
        actor = ActorGateway.create("runner", 1);
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
        
        actor.addActor("actor0", actor0, new Object());
        
        
        assertEquals("ready", direct.readMessagePayloadOnly("direct"));
        
        direct.writeMessage("runner:actor0", 1);
        assertEquals("saw integer", direct.readMessagePayloadOnly("direct"));
        direct.writeMessage("runner:actor0", "hi!");
        assertEquals("saw other", direct.readMessagePayloadOnly("direct"));
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
        
        actor.addActor("actor0", actor0, new Object());
        
        
        assertEquals("ready", direct.readMessagePayloadOnly("direct"));
        
        direct.writeMessage("runner:actor0", 1);
        assertEquals("saw integer", direct.readMessagePayloadOnly("direct"));
        assertEquals("got something!", direct.readMessagePayloadOnly("direct"));
        direct.writeMessage("runner:actor0", "hi!");
        assertEquals("got something!", direct.readMessagePayloadOnly("direct"));
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
        
        actor.addActor("actor0", actor0, new Object());
        
        
        assertEquals("ready", direct.readMessagePayloadOnly("direct"));
        
        direct.writeMessage("runner:actor0", 1);
        assertEquals("saw integer", direct.readMessagePayloadOnly("direct"));
        direct.writeMessage("runner:actor0", "hi!");
        assertNull(direct.readMessagePayloadOnly("direct", 500, TimeUnit.MILLISECONDS));
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
        
        actor.addActor("actor0", actor0, new Object());
        
        
        assertEquals("ready", direct.readMessagePayloadOnly("direct"));
        
        direct.writeMessage("runner:actor0", 1);
        assertEquals("saw integer", direct.readMessagePayloadOnly("direct"));
        assertEquals("got something!", direct.readMessagePayloadOnly("direct"));
        direct.writeMessage("runner:actor0", 2);
        assertEquals("saw integer again", direct.readMessagePayloadOnly("direct"));
        assertEquals("got something!", direct.readMessagePayloadOnly("direct"));
        direct.writeMessage("runner:actor0", 4);
        assertEquals("got something!", direct.readMessagePayloadOnly("direct"));
        assertNull(direct.readMessagePayloadOnly("direct", 500, TimeUnit.MILLISECONDS));
    }
}
