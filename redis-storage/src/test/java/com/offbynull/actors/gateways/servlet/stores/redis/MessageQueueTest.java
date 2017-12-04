package com.offbynull.actors.gateways.servlet.stores.redis;

import com.offbynull.actors.redisclient.Connection;
import com.offbynull.actors.redisclient.Connector;
import com.offbynull.actors.redisclients.test.TestConnector;
import com.offbynull.actors.shuttle.Address;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class MessageQueueTest {
    
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
    public void mustWriteAndReadToMessageQueue() throws Exception {
        MessageQueue queue = new MessageQueue(connection, Address.fromString("servlet:client1"), 60000L);
        
        byte[] msgData1 = new byte[] { 3, 4, 5 };
        byte[] msgData2 = new byte[] { 5, 6, 7 };
        byte[] msgData3 = new byte[] { 8, 9, 10 };

        queue.putMessage(msgData1);
        queue.putMessage(msgData2);
        queue.putMessage(msgData3);
        
        byte[] out;
        
        out = queue.take();
        assertArrayEquals(msgData1, out);
        
        out = queue.take();
        assertArrayEquals(msgData2, out);
        
        out = queue.take();
        assertArrayEquals(msgData3, out);
        
        out = queue.take();
        assertNull(out);
    }
    
    @Test
    public void mustDiscardMessageQueueOnTimeout() throws Exception {
        MessageQueue queue = new MessageQueue(connection, Address.fromString("servlet:client1"), 300L);
        
        byte[] msgData1 = new byte[] { 3, 4, 5 };
        byte[] msgData2 = new byte[] { 5, 6, 7 };
        byte[] msgData3 = new byte[] { 8, 9, 10 };

        queue.putMessage(msgData1);
        queue.putMessage(msgData2);
        queue.putMessage(msgData3);
        
        byte[] out;
        
        out = queue.take();
        assertArrayEquals(msgData1, out);
        
        Thread.sleep(400L);
        
        out = queue.take();
        assertNull(out);
    }
    
}
