package com.offbynull.peernetic.core.actor;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttles.test.CaptureShuttle;
import com.offbynull.peernetic.core.shuttles.test.NullShuttle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class ActorRunnerTest {

    private ActorRunner fixture;

    @Before
    public void setUp() {
        fixture = new ActorRunner("local");
    }

    @After
    public void tearDown() throws Exception {
        fixture.close();
    }

    @Test
    public void mustCommunicateBetweenActorsWithinSameActorRunner() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        fixture.addActor(
                "echoer",
                (Continuation cnt) -> {
                    Context ctx = (Context) cnt.getContext();
                    ctx.addOutgoingMessage(Address.fromString("local:sender"), ctx.getIncomingMessage());
                });
        fixture.addActor(
                "sender",
                (Continuation cnt) -> {
                    Context ctx = (Context) cnt.getContext();
                    ctx.addOutgoingMessage(Address.fromString("local:echoer"), "hi");
                    
                    cnt.suspend();
                    
                    assertEquals(ctx.getIncomingMessage(), "hi");
                    latch.countDown();
                },
                new Object());
        
        boolean processed = latch.await(5L, TimeUnit.SECONDS);
        assertTrue(processed);
    }

    @Test
    public void mustCommunicateBetweenActorsWithinDifferentActorRunners() throws Exception {
        try (ActorRunner secondaryActorRunner = new ActorRunner("local2")) {
            // Wire together
            fixture.addOutgoingShuttle(secondaryActorRunner.getIncomingShuttle());
            secondaryActorRunner.addOutgoingShuttle(fixture.getIncomingShuttle());
            
            // Test
            CountDownLatch latch = new CountDownLatch(1);
            secondaryActorRunner.addActor(
                    "echoer",
                    (Continuation cnt) -> {
                        Context ctx = (Context) cnt.getContext();
                        ctx.addOutgoingMessage(ctx.getSource(), ctx.getIncomingMessage());
                    });
            fixture.addActor(
                    "sender",
                    (Continuation cnt) -> {
                        Context ctx = (Context) cnt.getContext();
                        ctx.addOutgoingMessage(Address.fromString("local2:echoer"), "hi");
                        cnt.suspend();
                        
                        assertEquals(ctx.getIncomingMessage(), "hi");
                        latch.countDown();
                    },
                    new Object());
            
            boolean processed = latch.await(5L, TimeUnit.SECONDS);
            assertTrue(processed);
        }
    }
    
    @Test
    public void mustFailWhenAddingActorWithSameName() throws Exception {
        fixture.addActor("actor", (Context ctx) -> false);
        fixture.addActor("actor", (Context ctx) -> false);
        fixture.join();
    }
    
    @Test
    public void mustFailWhenRemoveActorThatDoesntExist() throws Exception {
        fixture.removeActor("actor");
        fixture.join();
    }

    @Test
    public void mustFailWhenAddingOutgoingShuttleWithSameName() throws Exception {
        NullShuttle shuttle = new NullShuttle("local");
        // Queue outgoing shuttle with prefix as ourselves ("local") be added. We won't be notified of rejection right away, but the add
        // will cause ActorRunner's background thread to throw an exception once its attempted. As such, join() will return indicating that
        // the thread died.
        fixture.addOutgoingShuttle(shuttle);
        fixture.join();
    }

    @Test
    public void mustFailWhenAddingConflictingOutgoingShuttle() throws Exception {
        // Should get added
        CaptureShuttle captureShuttle = new CaptureShuttle("fake");
        fixture.addOutgoingShuttle(captureShuttle);
        
        // Should cause a failure
        NullShuttle nullShuttle = new NullShuttle("fake");
        fixture.addOutgoingShuttle(nullShuttle);
        
        
        fixture.join();
    }
    
    @Test
    public void mustRemoveOutgoingShuttle() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        CaptureShuttle captureShuttle = new CaptureShuttle("fake");
        fixture.addOutgoingShuttle(captureShuttle);
        fixture.removeOutgoingShuttle("fake");
        
        fixture.addActor(
                "sender",
                (Continuation cnt) -> {
                    Context ctx = (Context) cnt.getContext();
                    ctx.addOutgoingMessage(Address.fromString("fake"), "1");
                    ctx.addOutgoingMessage(Address.fromString("local:sender"), new Object());
        
                    // Suspend here. We'll continue when we get the msg we sent to ourselves, and at that point we can be sure that msgs to
                    // "fake" were sent
                    cnt.suspend();
                    
                    latch.countDown();
                },
                new Object());

        boolean processed = latch.await(5L, TimeUnit.SECONDS);
        assertTrue(processed);
        
        assertTrue(captureShuttle.drainMessages().isEmpty());
    }
    
    @Test
    public void mustFailWhenRemovingIncomingShuttleThatDoesntExist() throws Exception {
        // Should cause a failure
        fixture.removeOutgoingShuttle("fake");
        fixture.join();
    }

    @Test
    public void mustStillRunIfOutgoingShuttleRemovedThenAddedAgain() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        CaptureShuttle captureShuttle = new CaptureShuttle("fake");
        fixture.addOutgoingShuttle(captureShuttle);
        fixture.removeOutgoingShuttle("fake");
        fixture.addOutgoingShuttle(captureShuttle);
        
        fixture.addActor(
                "sender",
                (Continuation cnt) -> {
                    Context ctx = (Context) cnt.getContext();
                    ctx.addOutgoingMessage(Address.fromString("fake"), "1");
                    ctx.addOutgoingMessage(Address.fromString("local:sender"), new Object());
        
                    // Suspend here. We'll continue when we get the msg we sent to ourselves, and at that point we can be sure that msgs to
                    // "fake" were sent
                    cnt.suspend();
                    
                    latch.countDown();
                },
                new Object());

        boolean processed = latch.await(5L, TimeUnit.SECONDS);
        assertTrue(processed);
        
        assertEquals("1", captureShuttle.drainMessages().get(0).getMessage());
    }
}
