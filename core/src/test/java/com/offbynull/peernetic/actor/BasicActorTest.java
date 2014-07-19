package com.offbynull.peernetic.actor;

import java.time.Instant;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import org.mockito.Mockito;

public final class BasicActorTest {

    @Test
    public void basicActorsTest() throws Throwable {
        Actor actor1 = Mockito.mock(Actor.class);
        Actor actor2 = Mockito.mock(Actor.class);
        ActorRunnable actorRunnable = ActorRunnable.createAndStart(actor1, actor2);

        Mockito.verify(actor1, Mockito.timeout(1000)).onStart(any(Instant.class));
        Mockito.verify(actor2, Mockito.timeout(1000)).onStart(any(Instant.class));
        
        
        
        Endpoint endpoint1 = actorRunnable.getEndpoint(actor1);
        Endpoint endpoint2 = actorRunnable.getEndpoint(actor2);
        
        endpoint1.send(NullEndpoint.INSTANCE, 1);
        endpoint1.send(NullEndpoint.INSTANCE, 2);
        endpoint1.send(NullEndpoint.INSTANCE, 3);
        Mockito.verify(actor1, Mockito.timeout(1000)).onStep(any(Instant.class), eq(NullEndpoint.INSTANCE), eq(1));
        Mockito.verify(actor1, Mockito.timeout(1000)).onStep(any(Instant.class), eq(NullEndpoint.INSTANCE), eq(2));
        Mockito.verify(actor1, Mockito.timeout(1000)).onStep(any(Instant.class), eq(NullEndpoint.INSTANCE), eq(3));
        
        endpoint2.send(NullEndpoint.INSTANCE, 1);
        endpoint2.send(NullEndpoint.INSTANCE, 2);
        endpoint2.send(NullEndpoint.INSTANCE, 3);
        Mockito.verify(actor2, Mockito.timeout(1000)).onStep(any(Instant.class), eq(NullEndpoint.INSTANCE), eq(1));
        Mockito.verify(actor2, Mockito.timeout(1000)).onStep(any(Instant.class), eq(NullEndpoint.INSTANCE), eq(2));
        Mockito.verify(actor2, Mockito.timeout(1000)).onStep(any(Instant.class), eq(NullEndpoint.INSTANCE), eq(3));
        
        
        
        actorRunnable.shutdown();
        
        Mockito.verify(actor1, Mockito.timeout(1000)).onStop(any(Instant.class));
        Mockito.verify(actor2, Mockito.timeout(1000)).onStop(any(Instant.class));
    }    
}
