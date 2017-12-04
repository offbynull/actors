
package com.offbynull.actors.gateways.servlet.stores.redis;

import com.offbynull.actors.redisclient.Connector;
import com.offbynull.actors.redisclients.test.TestConnector;
import com.offbynull.actors.shuttle.Message;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class RedisStoreTest {
    
    private RedisStore fixture;
    
    @Before
    public void before() {
//        try (Jedis jedis = new Jedis("192.168.206.110", 6379)) {
//            jedis.flushDB();
//        }
//        Connector connector = new JedisPoolConnector("192.168.206.110", 6379, 5);
        
        Connector connector = new TestConnector();
        
        fixture = RedisStore.create("servlet", connector, 300L);
    }
    
    @After
    public void after() throws Exception {
        fixture.close();
    }

    @Test
    public void mustWriteAndReadToMessageQueue() throws Exception {
        List<Message> inMsgs = Arrays.asList(
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"));
        List<Message> outMsgs;
        
        fixture.write("b", inMsgs);
        outMsgs = fixture.read("b");
        
        assertMessagesEquals(inMsgs, outMsgs);
    }

    @Test
    public void mustWriteAndReadToDifferentMessageQueue() throws Exception {
        List<Message> inMsgsA = Arrays.asList(
                new Message("actor:a:1:2:3", "servlet:a:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:a:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:a:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:a:2:3:4", "payload"));
        List<Message> inMsgsB = Arrays.asList(
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"));
        List<Message> outMsgsA;
        List<Message> outMsgsB;
        
        fixture.write("a", inMsgsA);
        fixture.write("b", inMsgsB);
        outMsgsA = fixture.read("a");
        outMsgsB = fixture.read("b");
        
        assertMessagesEquals(inMsgsA, outMsgsA);
        assertMessagesEquals(inMsgsB, outMsgsB);
    }

    @Test
    public void mustTakeEmptyListForNonExistantQueue() throws Exception {
        List<Message> outMsgsA = fixture.read("a");
        List<Message> outMsgsB = fixture.read("b");
        
        assertTrue(outMsgsA.isEmpty());
        assertTrue(outMsgsB.isEmpty());
    }
    
    @Test
    public void mustDiscardMessageQueueOnTimeout() throws Exception {
        List<Message> inMsgsA = Arrays.asList(
                new Message("actor:a:1:2:3", "servlet:a:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:a:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:a:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:a:2:3:4", "payload"));
        List<Message> inMsgsB = Arrays.asList(
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"));
        List<Message> outMsgsA;
        List<Message> outMsgsB;
        
        
        
        fixture.write("a", inMsgsA);
        fixture.write("b", inMsgsB);

        Thread.sleep(400L);
        
        outMsgsA = fixture.read("a");
        outMsgsB = fixture.read("b");

        assertTrue(outMsgsA.isEmpty());
        assertTrue(outMsgsB.isEmpty());
        
        
        
        fixture.write("a", inMsgsA);
        fixture.write("b", inMsgsB);
        
        outMsgsA = fixture.read("a");
        outMsgsB = fixture.read("b");
        
        assertMessagesEquals(inMsgsA, outMsgsA);
        assertMessagesEquals(inMsgsB, outMsgsB);
    }
    
    @Test
    public void mustStillAddAfterMessageQueueTimeout() throws Exception {
        List<Message> inMsgsA = Arrays.asList(
                new Message("actor:a:1:2:3", "servlet:a:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:a:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:a:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:a:2:3:4", "payload"));
        List<Message> inMsgsB = Arrays.asList(
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"));
        
        fixture.write("a", inMsgsA);
        fixture.write("b", inMsgsB);

        Thread.sleep(400L);
        
        List<Message> outMsgsA = fixture.read("a");
        List<Message> outMsgsB = fixture.read("b");
        
        fixture.write("a", inMsgsA);
        fixture.write("b", inMsgsB);

        assertTrue(outMsgsA.isEmpty());
        assertTrue(outMsgsB.isEmpty());
    }
    
    private void assertMessagesEquals(List<Message> expected, List<Message> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            Message expectedMsg = expected.get(i);
            Message actualMsg = actual.get(i);
            
            assertEquals(expectedMsg.getSourceAddress(), actualMsg.getSourceAddress());
            assertEquals(expectedMsg.getDestinationAddress(), actualMsg.getDestinationAddress());
            assertEquals(expectedMsg.getMessage(), actualMsg.getMessage());
        }
    }
    
}
