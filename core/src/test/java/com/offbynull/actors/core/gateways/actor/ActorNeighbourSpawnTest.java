
package com.offbynull.actors.core.gateways.actor;

import com.offbynull.actors.core.gateways.direct.DirectGateway;
import com.offbynull.coroutines.user.Coroutine;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.Before;

public class ActorNeighbourSpawnTest {
    
    private ActorGateway runner;
    private DirectGateway direct;

    @Before
    public void setUp() {
        runner = ActorGateway.create("runner", 1);
        direct = DirectGateway.create("direct");
        
        direct.addOutgoingShuttle(runner.getIncomingShuttle());
        runner.addOutgoingShuttle(direct.getIncomingShuttle());
        
        direct.listen("direct");
    }
    
    @After
    public void tearDown() throws Exception {
        runner.close();
        direct.close();
    }
    
    @Test(timeout = 2000L)
    public void mustSpawnRootActorFromAnotherRootActor() throws Exception {
        Coroutine actor1 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            
            ctx.out("direct", "ready from 1");
        };
        
        Coroutine actor0 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            ctx.out("direct", "ready from 0");

            ctx.root("actor1", actor1, new Object());
        };
        
        runner.addActor("actor0", actor0, new Object());
        
        
        assertEquals("ready from 0", direct.readMessagePayloadOnly("direct"));
        assertEquals("ready from 1", direct.readMessagePayloadOnly("direct"));
    }
}
