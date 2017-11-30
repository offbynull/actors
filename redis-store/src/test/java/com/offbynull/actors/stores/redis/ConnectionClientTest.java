package com.offbynull.actors.stores.redis;

import static com.offbynull.actors.shuttle.Address.fromString;
import com.offbynull.actors.stores.redis.client.ActorAccessor;
import com.offbynull.actors.stores.redis.client.ActorAccessor.Work;
import com.offbynull.actors.stores.redis.client.Client;
import com.offbynull.actors.stores.redis.client.ClientFactory;
import com.offbynull.actors.stores.redis.client.TimestampQueue;
import com.offbynull.actors.stores.redis.connector.Connector;
import com.offbynull.actors.stores.redis.connectors.jedis.JedisPoolConnector;
import org.junit.After;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

public final class ConnectionClientTest {
    
    private Connector connector;
    private ClientFactory clientFactory;
    private Client client;

    
    @Before
    public void setUp() throws Exception {
        connector = new JedisPoolConnector("192.168.56.101", 6379, 1);
        clientFactory = new ConnectorClientFactory(connector);
//        connector = new TestConnector();
//        clientFactory = new ConnectorClientFactory(connector);
        client = clientFactory.getClient();
    }
    
    @After
    public void tearDown() throws Exception {
        client.close();
        clientFactory.close();
        connector.close();
    }

    @Test
    public void mustProperlyPullFromTimestampQueue() throws Exception {
        TimestampQueue rtq = client.getMessageCheckQueue(1);

        rtq.insert(0, fromString("test1:test2:a"));
        rtq.insert(5, fromString("test1:test2:d"));
        rtq.insert(2, fromString("test1:test2:c"));
        rtq.insert(1, fromString("test1:test2:b"));
        
        assertEquals(fromString("test1:test2:a"), rtq.remove(10L)); 
        assertEquals(fromString("test1:test2:b"), rtq.remove(10L)); 
        assertEquals(fromString("test1:test2:c"), rtq.remove(10L)); 
        assertEquals(fromString("test1:test2:d"), rtq.remove(10L)); 
        assertNull(rtq.remove(10L)); 
    }

    @Test
    public void mustProperlyWriteAndReadMessages() throws Exception {
        byte[] actorData = new byte[] { 1, 2, 3 };
        byte[] msgData1 = new byte[] { 3, 4, 5 };
        byte[] msgData2 = new byte[] { 5, 6, 7 };
        byte[] msgData3 = new byte[] { 8, 9, 10 };

        ActorAccessor raa = client.getActorAccessor(fromString("test1:a"));
        raa.remove();

        raa.update(actorData, null, -1L, 0);
        raa.putMessage(msgData1);
        raa.putMessage(msgData2);
        
        ActorAccessor.Work pm;
        pm = raa.nextMessage();
        assertArrayEquals(msgData1, pm.getMessageData());
        assertArrayEquals(actorData, pm.getActorData());
        raa.putMessage(msgData3);
        
        pm = raa.nextMessage();
        assertNull(pm);
        
        raa.update(actorData, null, -1L, 0);
        pm = raa.nextMessage();
        assertArrayEquals(msgData2, pm.getMessageData());
        assertArrayEquals(actorData, pm.getActorData());
        
        raa.remove();
    }

    @Test
    public void mustProperlyHitCheckpoint() throws Exception {
        byte[] actorData = new byte[] { 1, 2, 3 };
        byte[] checkpointMsg = new byte[] { 3, 4, 5 };

        ActorAccessor raa = client.getActorAccessor(fromString("test1:b"));
        raa.remove();

        raa.update(actorData, checkpointMsg, 0L, 0);
        
        Work pm;

        pm = raa.nextMessage();
        assertNull(pm);
        
        pm = raa.checkpointMessage(0L);
        assertArrayEquals(checkpointMsg, pm.getMessageData());
        assertArrayEquals(actorData, pm.getActorData());

        pm = raa.nextMessage();
        assertNull(pm);

        raa.remove();
    }
    
}
