package com.offbynull.actors.gateways.actor.stores.redis;

import com.offbynull.actors.gateways.actor.SerializableActor;
import com.offbynull.actors.gateways.actor.SerializableActorHelper;
import com.offbynull.actors.gateways.actor.Store.StoredWork;
import com.offbynull.actors.shuttle.Message;
import com.offbynull.actors.redisclient.Connector;
import com.offbynull.actors.redisclients.test.TestConnector;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.Before;

public class RedisStoreTest {

    private RedisStore fixture;
    
    @Before
    public void before() {
//        try (Jedis jedis = new Jedis("192.168.56.101", 6379)) {
//            jedis.flushDB();
//        }
//        Connector connector = new JedisPoolConnector("192.168.56.101", 6379, 5);
        
        Connector connector = new TestConnector();
        
        fixture = RedisStore.create("actor", connector, new QueueCountController(1), new QueueCountController(1));
    }
    
    @After
    public void after() throws Exception {
        fixture.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void mustFailWhenStoringMessageWithDestinationThatHasBadPrefix() {
        fixture.store(new Message("unknown1:a", "unknown2:b:2:3:4", "payload"));
    }

    @Test
    public void mustAllowWhenStoringMessagesWithSourceThatHasAnyPrefix() {
        fixture.store(new Message("unknown1:a", "actor:b:2:3:4", "payload"));
    }

    @Test
    public void mustAllowWhenStoringMessagesWithWithSingleElementSource() {
        fixture.store(new Message("unknown1", "actor:b:2:3:4", "payload"));
    }

    @Test
    public void mustIgnoreMessagesComingInForActorsThatDontExist() {
        fixture.store(
                new Message("actor:a:1:2:3", "actor:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "actor:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "actor:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "actor:b:2:3:4", "payload"));
    }

    @Test
    public void mustStoreActor() {
        SerializableActor actor = SerializableActorHelper.createFake("actor:b");
        fixture.store(actor);
    }    

    @Test
    public void mustStoreMessagesComingInForActorsThatExist() {
        SerializableActor actor = SerializableActorHelper.createFake("actor:b");
        fixture.store(actor);
        fixture.store(
                new Message("actor:a:1:2:3", "actor:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "actor:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "actor:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "actor:b:2:3:4", "payload"));
    } 

    @Test(timeout = 1000L)
    public void mustPullWork() {
        SerializableActor actor = SerializableActorHelper.createFake("actor:b");
        fixture.store(actor);
        fixture.store(
                new Message("actor:a:1:1", "actor:b:2:1", "payload1"),
                new Message("actor:a:1:2", "actor:b:2:2", "payload2"),
                new Message("actor:a:1:3", "actor:b:2:3", "payload3"),
                new Message("actor:a:1:4", "actor:b:2:4", "payload4"));
        StoredWork work = fixture.take();
        
        assertEquals("actor:a:1:1", work.getMessage().getSourceAddress().toString());
        assertEquals("actor:b:2:1", work.getMessage().getDestinationAddress().toString());
        assertEquals("payload1", work.getMessage().getMessage());
    } 

    @Test(timeout = 10000000000L)
    public void mustRestoreAfterPulllingWork() {
        SerializableActor actor = SerializableActorHelper.createFake("actor:b");
        fixture.store(actor);
        fixture.store(
                new Message("actor:a:1:1", "actor:b:2:1", "payload1"),
                new Message("actor:a:1:2", "actor:b:2:2", "payload2"),
                new Message("actor:a:1:3", "actor:b:2:3", "payload3"),
                new Message("actor:a:1:4", "actor:b:2:4", "payload4"));
        fixture.take();
        fixture.store(actor);
        
        StoredWork work = fixture.take();
        
        assertEquals("actor:a:1:2", work.getMessage().getSourceAddress().toString());
        assertEquals("actor:b:2:2", work.getMessage().getDestinationAddress().toString());
        assertEquals("payload2", work.getMessage().getMessage());
    } 

    @Test(timeout = 1000L)
    public void mustDiscardActor() {
        SerializableActor actorA = SerializableActorHelper.createFake("actor:a");
        SerializableActor actorB = SerializableActorHelper.createFake("actor:b");
        fixture.store(actorA);
        fixture.store(actorB);
        
        fixture.discard("actor:b");
    } 

    @Test(timeout = 2000L)
    public void mustCheckpointActor() {
        SerializableActor actor = SerializableActorHelper.createFake("actor:a", "timeout_msg", 300L);
        fixture.store(actor);
        
        StoredWork work = fixture.take();
        
        assertEquals("actor:a", work.getMessage().getSourceAddress().toString());
        assertEquals("actor:a", work.getMessage().getDestinationAddress().toString());
        assertEquals("timeout_msg", work.getMessage().getMessage());
    } 

    @Test(timeout = 2000L)
    public void mustNotAllowRecoveryOfOldCheckpointInstanceToBlowAwayState() {
        boolean stored;
        
        SerializableActor initialActor = SerializableActorHelper.createFake("actor:a", "timeout_msg", 300L);
        stored = fixture.store(initialActor);
        assertTrue(stored);
        
        SerializableActor checkpointHitActor = fixture.take().getActor();
        assertEquals(1, checkpointHitActor.getCheckpointInstance());
        
        // Add the checkpointHit actor back in (with new checkpoint details), and try to replace it with initialActor...
        
        stored = fixture.store(checkpointHitActor);
        assertTrue(stored);
        stored = fixture.store(initialActor);
        assertFalse(stored);

        SerializableActor checkpointHitActor2 = fixture.take().getActor();
        assertEquals(2, checkpointHitActor2.getCheckpointInstance()); // Another checkpoint hit after we put it back in.
    }
    
    @Test(timeout = 2000L)
    public void mustHitCheckpointEvenIfInTheMiddleOfProcessingAMessage() throws Exception {
        SerializableActor initialActor = SerializableActorHelper.createFake("actor:a", "timeout_msg", 300L);
        fixture.store(initialActor);
        
        fixture.store(
                new Message("actor:b:1:1", "actor:a:2:1", "payload1"),
                new Message("actor:b:1:2", "actor:a:2:2", "payload2"),
                new Message("actor:b:1:3", "actor:a:2:3", "payload3"),
                new Message("actor:b:1:4", "actor:a:2:4", "payload4"));
        
        SerializableActor msgRecvdActor = fixture.take().getActor();
        assertEquals(0, msgRecvdActor.getCheckpointInstance());
        
        // SLEEP UNTIL THE CHECKPOINT SHOULD TRIGGER THEN GET THE ACTOR AGAIN, RESULT SHOULD BE CHECKPOINT MESSAGE
        //    note that we aren't putting the actor back in here, so checkpoint time shouldn't reset
        Thread.sleep(400L);
        
        SerializableActor checkpointHitActor = fixture.take().getActor();
        assertEquals(1, checkpointHitActor.getCheckpointInstance());
    }
}
