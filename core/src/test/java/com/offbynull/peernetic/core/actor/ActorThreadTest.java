package com.offbynull.peernetic.core.actor;

import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.shuttles.test.CaptureShuttle;
import com.offbynull.peernetic.core.shuttles.test.NullShuttle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class ActorThreadTest {

    private ActorThread fixture;

    @Before
    public void setUp() {
        fixture = ActorThread.create("local");
    }

    @After
    public void tearDown() throws Exception {
        fixture.close();
    }

    @Test
    public void mustBeAbleToCommunicateBetweenActorsWithinSameActorThread() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        fixture.addCoroutineActor(
                "echoer",
                cnt -> {
                    Context ctx = (Context) cnt.getContext();
                    ctx.addOutgoingMessage("local:sender", ctx.getIncomingMessage());
                });
        fixture.addCoroutineActor(
                "sender",
                cnt -> {
                    Context ctx = (Context) cnt.getContext();
                    ctx.addOutgoingMessage("local:echoer", "hi");
                    
                    cnt.suspend();
                    
                    assertEquals(ctx.getIncomingMessage(), "hi");
                    latch.countDown();
                },
                new Object());
        
        boolean processed = latch.await(5L, TimeUnit.SECONDS);
        assertTrue(processed);
    }

    @Test
    public void mustBeAbleToCommunicateBetweenActorsWithinDifferentActorThreads() throws Exception {
        try (ActorThread secondaryActorThread = ActorThread.create("local2")) {
            // Wire together
            fixture.addOutgoingShuttle(secondaryActorThread.getIncomingShuttle());
            secondaryActorThread.addOutgoingShuttle(fixture.getIncomingShuttle());
            
            // Test
            CountDownLatch latch = new CountDownLatch(1);
            secondaryActorThread.addCoroutineActor(
                    "echoer",
                    cnt -> {
                        Context ctx = (Context) cnt.getContext();
                        ctx.addOutgoingMessage(ctx.getSource(), ctx.getIncomingMessage());
                    });
            fixture.addCoroutineActor(
                    "sender",
                    cnt -> {
                        Context ctx = (Context) cnt.getContext();
                        ctx.addOutgoingMessage("local2:echoer", "hi");
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
    public void mustRejectAddingOutgoingShuttleWithSameName() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        NullShuttle shuttle = new NullShuttle("local");
        // Queue outgoing shuttle with prefix as ourselves ("local") be added. We won't be notified of rejection here, but the add will be
        // rejected once its attempted. As such, we can test to see if the add failed if "local" still refers to fixture.
        fixture.addOutgoingShuttle(shuttle);
        
        fixture.addCoroutineActor(
                "test",
                cnt -> {
                    Context ctx = (Context) cnt.getContext();
                    ctx.addOutgoingMessage("local:test", "hi");
                    
                    cnt.suspend();
                    
                    assertEquals(ctx.getIncomingMessage(), "hi");
                    latch.countDown();
                },
                new Object());
        
        boolean processed = latch.await(5L, TimeUnit.SECONDS);
        assertTrue(processed);
    }

    @Test
    public void mustRejectAddingConflictingOutgoingShuttle() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        // Should get added
        CaptureShuttle captureShuttle = new CaptureShuttle("fake");
        fixture.addOutgoingShuttle(captureShuttle);
        
        // Should get ignored
        NullShuttle nullShuttle = new NullShuttle("fake");
        fixture.addOutgoingShuttle(nullShuttle);
        
        
        fixture.addCoroutineActor(
                "test",
                cnt -> {
                    Context ctx = (Context) cnt.getContext();
                    ctx.addOutgoingMessage("fake", "hi");
                    latch.countDown();
                },
                new Object());
        
        boolean processed = latch.await(5L, TimeUnit.SECONDS);
        assertTrue(processed);
        
        Message message = captureShuttle.takeNextMessage();
        assertEquals("fake", message.getDestinationAddress());
        assertEquals("local:test", message.getDestinationAddress());
        assertEquals("hi", message.getMessage());
    }

}
