package com.offbynull.actors.core.stores.memory;

import com.offbynull.actors.core.stores.memory.MemoryStore;
import com.offbynull.actors.core.gateways.actor.SerializableActor;
import com.offbynull.actors.core.gateways.actor.SerializableActorHelper;
import com.offbynull.actors.core.store.StoredWork;
import com.offbynull.actors.core.shuttle.Message;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

public class MemoryStoreTest {

    private MemoryStore fixture;
    
    @Before
    public void before() {
        fixture = new MemoryStore("actor", 2);
    }
    
    @After
    public void after() {
        IOUtils.closeQuietly(fixture);
    }

    @Test(expected = IllegalStateException.class)
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
        assertEquals(0, fixture.getStoredMessageCount());
    }

    @Test
    public void mustStoreActor() {
        SerializableActor actor = SerializableActorHelper.createFake("actor:b");
        fixture.store(actor);
        assertEquals(1, fixture.getActorCount());
        assertEquals(0, fixture.getProcessingActorCount());
        assertEquals(0, fixture.getReadyActorCount());
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
        assertEquals(4, fixture.getStoredMessageCount());
        assertEquals(1, fixture.getActorCount());
        assertEquals(0, fixture.getProcessingActorCount());
        assertEquals(1, fixture.getReadyActorCount());
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
        
        assertEquals(3, fixture.getStoredMessageCount());
        assertEquals(1, fixture.getActorCount());
        assertEquals(1, fixture.getProcessingActorCount());
        assertEquals(0, fixture.getReadyActorCount());
    } 

    @Test(timeout = 1000L)
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
        
        assertEquals(3, fixture.getStoredMessageCount());
        assertEquals(1, fixture.getActorCount());
        assertEquals(0, fixture.getProcessingActorCount());
        assertEquals(1, fixture.getReadyActorCount());
        
        StoredWork work = fixture.take();
        
        assertEquals("actor:a:1:2", work.getMessage().getSourceAddress().toString());
        assertEquals("actor:b:2:2", work.getMessage().getDestinationAddress().toString());
        assertEquals("payload2", work.getMessage().getMessage());
        
        assertEquals(2, fixture.getStoredMessageCount());
        assertEquals(1, fixture.getActorCount());
        assertEquals(1, fixture.getProcessingActorCount());
        assertEquals(0, fixture.getReadyActorCount());
    } 

    @Test(timeout = 1000L)
    public void mustDiscardActor() {
        SerializableActor actorA = SerializableActorHelper.createFake("actor:a");
        SerializableActor actorB = SerializableActorHelper.createFake("actor:b");
        fixture.store(actorA);
        fixture.store(actorB);

        assertEquals(2, fixture.getActorCount());
        assertEquals(0, fixture.getProcessingActorCount());
        assertEquals(0, fixture.getReadyActorCount());
        
        fixture.discard("actor:b");
        
        assertEquals(1, fixture.getActorCount());
        assertEquals(0, fixture.getProcessingActorCount());
        assertEquals(0, fixture.getReadyActorCount());
    } 

    @Test(timeout = 1000L)
    public void mustCheckpointActor() {
        SerializableActor actor = SerializableActorHelper.createFake("actor:a", "timeout_msg", 300L);
        fixture.store(actor);
        
        StoredWork work = fixture.take();
        
        assertEquals("actor:a", work.getMessage().getSourceAddress().toString());
        assertEquals("actor:a", work.getMessage().getDestinationAddress().toString());
        assertEquals("timeout_msg", work.getMessage().getMessage());
    } 
}
