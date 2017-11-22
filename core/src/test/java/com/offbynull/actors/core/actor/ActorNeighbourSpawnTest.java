
package com.offbynull.actors.core.actor;

import com.offbynull.actors.core.gateways.direct.DirectGateway;
import com.offbynull.coroutines.user.Coroutine;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.Before;

public class ActorNeighbourSpawnTest {
    
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
    public void mustSpawnRootActorFromAnotherRootActor() throws Exception {
        Coroutine actor1 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.ruleSet().allowAll();
            
            ctx.out("direct", "ready from 1");
        };
        
        Coroutine actor0 = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.ruleSet().allowAll();
            ctx.out("direct", "ready from 0");

            ctx.neighbour("actor1", actor1, new Object());
        };
        
        runner.addActor("actor0", actor0, new Object());
        
        
        assertEquals("ready from 0", direct.readMessagePayloadOnly());
        assertEquals("ready from 1", direct.readMessagePayloadOnly());
    }
}
