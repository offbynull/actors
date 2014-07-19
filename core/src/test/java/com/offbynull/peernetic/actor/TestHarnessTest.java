package com.offbynull.peernetic.actor;

import java.time.Duration;
import java.time.Instant;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import org.mockito.Mockito;

public final class TestHarnessTest {
    
    public TestHarnessTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void basicTest() throws Throwable {
        TestHarness<String> harness = new TestHarness<>();
        
        Actor actor1 = Mockito.mock(Actor.class);
        Actor actor2 = Mockito.mock(Actor.class);
        
        harness.addActor("actor1", actor1, Instant.ofEpochMilli(0L));
        harness.addActor("actor2", actor2, Instant.ofEpochMilli(0L));
        Assert.assertTrue(harness.hasMore());
        harness.process();
        harness.process();
        Assert.assertFalse(harness.hasMore());
        Mockito.verify(actor1).onStart(any(Instant.class));
        Mockito.verify(actor2).onStart(any(Instant.class));
        
        harness.scheduleFromNull(Duration.ZERO, "actor1", 1);
        harness.scheduleFromNull(Duration.ZERO, "actor1", 2);
        harness.scheduleFromNull(Duration.ZERO, "actor1", 3);
        Assert.assertTrue(harness.hasMore());
        harness.process();
        harness.process();
        harness.process();
        Assert.assertFalse(harness.hasMore());
        Mockito.verify(actor1).onStep(any(Instant.class), eq(NullEndpoint.INSTANCE), eq(1));
        Mockito.verify(actor1).onStep(any(Instant.class), eq(NullEndpoint.INSTANCE), eq(2));
        Mockito.verify(actor1).onStep(any(Instant.class), eq(NullEndpoint.INSTANCE), eq(3));
        
        harness.scheduleFromNull(Duration.ZERO, "actor2", 1);
        harness.scheduleFromNull(Duration.ZERO, "actor2", 2);
        harness.scheduleFromNull(Duration.ZERO, "actor2", 3);
        Assert.assertTrue(harness.hasMore());
        harness.process();
        harness.process();
        harness.process();
        Assert.assertFalse(harness.hasMore());
        Mockito.verify(actor2).onStep(any(Instant.class), eq(NullEndpoint.INSTANCE), eq(1));
        Mockito.verify(actor2).onStep(any(Instant.class), eq(NullEndpoint.INSTANCE), eq(2));
        Mockito.verify(actor2).onStep(any(Instant.class), eq(NullEndpoint.INSTANCE), eq(3));
        
        harness.removeActor("actor1", Instant.ofEpochMilli(0L));
        harness.removeActor("actor2", Instant.ofEpochMilli(0L));
        Assert.assertTrue(harness.hasMore());
        harness.process();
        harness.process();
        Assert.assertFalse(harness.hasMore());
        Mockito.verify(actor1).onStop(any(Instant.class));
        Mockito.verify(actor2).onStop(any(Instant.class));
    }
    
    @Test
    public void scheduleSendBeforeJoinTest() throws Throwable {
        TestHarness<String> harness = new TestHarness<>();
        
        Actor actor1 = Mockito.mock(Actor.class);
        
        // schedule to send at 1
        harness.scheduleFromNull(Duration.ofSeconds(1L), "actor1", 1);
        
        // join at 0
        harness.addActor("actor1", actor1, Instant.ofEpochMilli(0L));
        harness.process();
        Mockito.verify(actor1).onStart(any(Instant.class));
        
        // verify send made it through
        harness.process();
        Mockito.verify(actor1).onStep(any(Instant.class), eq(NullEndpoint.INSTANCE), eq(1));
    }

    @Test
    public void scheduleSendAfterLeaveTest() throws Throwable {
        TestHarness<String> harness = new TestHarness<>();
        
        Actor actor1 = Mockito.mock(Actor.class);
        
        // schedule to send at 2
        harness.scheduleFromNull(Duration.ofMillis(2L), "actor1", 1);
        
        // join at 0
        harness.addActor("actor1", actor1, Instant.ofEpochMilli(0L));
        harness.process();
        Mockito.verify(actor1).onStart(any(Instant.class));
        
        // remove at 1
        harness.removeActor("actor1", Instant.ofEpochMilli(1L));
        harness.process();
        Mockito.verify(actor1).onStop(any(Instant.class));
        
        // see what happens at 2
        harness.process(); // should not crash here
    }

    @Test
    public void actorSendToNonExistingTest() throws Throwable {
        TestHarness<String> harness = new TestHarness<>();
        
        Actor actor1 = Mockito.mock(Actor.class);
        Actor actor2 = new SendActor(harness.getEndpointDirectory(), "actor2", "actor1");
        
        // add mock actor
        harness.addActor("actor1", actor1, Instant.ofEpochMilli(0L));
        harness.process();
        Mockito.verify(actor1).onStart(any(Instant.class));

        // add send actor
        harness.addActor("actor2", actor2, Instant.ofEpochMilli(0L));
        harness.process();
        Mockito.verify(actor1).onStart(any(Instant.class));
        
        // ensure mock actor got send actor's message
        harness.process();
        Mockito.verify(actor1).onStep(any(Instant.class), any(Endpoint.class), eq("hi"));
    }
    
    private static final class SendActor implements Actor {

        private EndpointDirectory<String> directory;
        private String self;
        private String who;

        public SendActor(EndpointDirectory<String> directory, String self, String who) {
            this.directory = directory;
            this.self = self;
            this.who = who;
        }
        
        @Override
        public void onStart(Instant time) throws Exception {
            Endpoint dst = directory.lookup(who);
            Endpoint src = directory.lookup(self);
            dst.send(src, "hi");
        }

        @Override
        public void onStep(Instant time, Endpoint source, Object message) throws Exception {
        }

        @Override
        public void onStop(Instant time) throws Exception {
        }
        
    }
}
