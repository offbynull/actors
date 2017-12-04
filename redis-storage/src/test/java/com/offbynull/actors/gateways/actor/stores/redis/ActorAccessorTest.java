package com.offbynull.actors.gateways.actor.stores.redis;

import com.offbynull.actors.gateways.actor.stores.redis.ActorAccessor.Work;
import com.offbynull.actors.redisclient.Connection;
import static com.offbynull.actors.shuttle.Address.fromString;
import com.offbynull.actors.redisclient.Connector;
import com.offbynull.actors.redisclients.test.TestConnector;
import org.junit.After;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

public final class ActorAccessorTest {
    
    private Connector connector;
    private Connection connection;

    
    @Before
    public void setUp() throws Exception {
//        connector = new JedisPoolConnector("192.168.56.101", 6379, 1);
//        clientFactory = new ConnectorClientFactory(connector);
        connector = new TestConnector();
        connection = connector.getConnection();
    }
    
    @After
    public void tearDown() throws Exception {
        connection.close();
        connector.close();
    }

    @Test
    public void mustProperlyWriteAndReadMessages() throws Exception {
        byte[] actorData = new byte[] { 1, 2, 3 };
        byte[] msgData1 = new byte[] { 3, 4, 5 };
        byte[] msgData2 = new byte[] { 5, 6, 7 };
        byte[] msgData3 = new byte[] { 8, 9, 10 };

        ActorAccessor raa = new ActorAccessor(connection, fromString("test1:a"));
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

        ActorAccessor raa = new ActorAccessor(connection, fromString("test1:b"));
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
