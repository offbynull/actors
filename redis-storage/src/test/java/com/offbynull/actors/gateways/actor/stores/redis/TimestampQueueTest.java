package com.offbynull.actors.gateways.actor.stores.redis;

import com.offbynull.actors.redisclient.Connection;
import static com.offbynull.actors.shuttle.Address.fromString;
import com.offbynull.actors.redisclient.Connector;
import com.offbynull.actors.redisclients.test.TestConnector;
import org.junit.After;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

public final class TimestampQueueTest {
    
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
    public void mustProperlyPullFromTimestampQueue() throws Exception {
        TimestampQueue rtq = new TimestampQueue(connection, "test", 1);

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
}
