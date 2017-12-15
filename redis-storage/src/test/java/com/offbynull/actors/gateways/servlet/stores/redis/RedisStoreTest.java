
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RedisStoreTest {
    
    private RedisStore fixture;
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    @Before
    public void before() {
//        try (Jedis jedis = new Jedis("192.168.206.110", 6379)) {
//            jedis.flushDB();
//        }
//        Connector connector = new JedisPoolConnector("192.168.206.110", 6379, 5);
        
        Connector connector = new TestConnector();
        
        fixture = RedisStore.create("servlet", connector, 3000L);
    }
    
    @After
    public void after() throws Exception {
        fixture.close();
    }

    @Test
    public void mustReturnEmptyListWhenDequeueOutOnIdThatDoesntExist() {
        List<Message> actual = fixture.dequeueOut("id", 0);
        assertTrue(actual.isEmpty());
    }

    @Test
    public void mustAllowKeepingOffsetSameForDequeueOut() {
        List<Message> expected = Arrays.asList(
                new Message("src:id1", "http:id", "payload1"),
                new Message("src:id2", "http:id", "payload2"),
                new Message("src:id3", "http:id", "payload3")
        );        
        fixture.queueOut("id", expected);

        List<Message> actual;

        actual = fixture.dequeueOut("id", 0);
        assertMessagesEquals(expected, actual);
        actual = fixture.dequeueOut("id", 0);
        assertMessagesEquals(expected, actual);
    }

    @Test
    public void mustAllowMovingOffsetForwardForDequeueOut() {
        List<Message> expected = Arrays.asList(
                new Message("src:id1", "http:id", "payload1"),
                new Message("src:id2", "http:id", "payload2"),
                new Message("src:id3", "http:id", "payload3")
        );        
        fixture.queueOut("id", expected);
        
        List<Message> actual;

        actual = fixture.dequeueOut("id", 0);
        assertMessagesEquals(expected, actual);
        actual = fixture.dequeueOut("id", 2);
        assertMessagesEquals(expected.subList(2, 3), actual);
        actual = fixture.dequeueOut("id", 3);
        assertTrue(actual.isEmpty());
    }

    @Test
    public void mustFailMovingOffsetBackwardForDequeueOut() {
        List<Message> expected = Arrays.asList(
                new Message("src:id1", "http:id", "payload1"),
                new Message("src:id2", "http:id", "payload2"),
                new Message("src:id3", "http:id", "payload3")
        );        
        fixture.queueOut("id", expected);
        
        List<Message> actual;

        actual = fixture.dequeueOut("id", 0);
        assertMessagesEquals(expected, actual);
        actual = fixture.dequeueOut("id", 2);
        assertMessagesEquals(expected.subList(2, 3), actual);
        expectedException.expect(IllegalStateException.class);
        fixture.dequeueOut("id", 0);
    }








    @Test
    public void mustReturnEmptyOnInitialDequeueIn() {
        List<Message> actual = fixture.dequeueIn("id");
        assertTrue(actual.isEmpty());
    }
    
    @Test
    public void mustBulkAddOnQueueInAndReturnAllOnDequeueIn() {
        List<Message> expected = Arrays.asList(
                new Message("http:id", "dst:id1", "payload1"),
                new Message("http:id", "dst:id2", "payload2"),
                new Message("http:id", "dst:id3", "payload3")
        );        

        fixture.queueIn("id", 0, expected);
        List<Message> actual = fixture.dequeueIn("id");

        assertMessagesEquals(expected, actual);
    }

    @Test
    public void mustIncrementallyAddOnQueueInAndReturnAllOnDequeueIn() {
        List<Message> expected = Arrays.asList(
                new Message("http:id", "dst:id1", "payload1"),
                new Message("http:id", "dst:id2", "payload2"),
                new Message("http:id", "dst:id3", "payload3")
        );        

        fixture.queueIn("id", 0, expected.subList(0, 1));
        fixture.queueIn("id", 1, expected.subList(1, 2));
        fixture.queueIn("id", 2, expected.subList(2, 3));
        List<Message> actual = fixture.dequeueIn("id");

        assertMessagesEquals(expected, actual);
    }

    @Test
    public void mustReturnEmptyOnSubsequentDequeueIn() {
        List<Message> expected = Arrays.asList(
                new Message("http:id", "dst:id1", "payload1"),
                new Message("http:id", "dst:id2", "payload2"),
                new Message("http:id", "dst:id3", "payload3")
        );        

        fixture.queueIn("id", 0, expected);
        List<Message> actual;
        
        actual = fixture.dequeueIn("id");
        assertMessagesEquals(expected, actual);
        
        actual = fixture.dequeueIn("id");
        assertTrue(actual.isEmpty());
    }

    @Test
    public void mustDequeueInAndRequeueIn() {
        List<Message> actual;
        List<Message> expected;
        
        
        
        expected = Arrays.asList(
                new Message("http:id", "dst:id1", "payload1"),
                new Message("http:id", "dst:id2", "payload2"),
                new Message("http:id", "dst:id3", "payload3")
        );        

        fixture.queueIn("id", 0, expected);
        
        actual = fixture.dequeueIn("id");
        assertMessagesEquals(expected, actual);

        
        
        expected = Arrays.asList(
                new Message("http:id", "dst:id4", "payload4"),
                new Message("http:id", "dst:id5", "payload5"),
                new Message("http:id", "dst:id6", "payload6")
        );        
        
        fixture.queueIn("id", 3, expected);
        
        actual = fixture.dequeueIn("id");
        assertMessagesEquals(expected, actual);
        
        
        
        actual = fixture.dequeueIn("id");
        assertTrue(actual.isEmpty());
    }
    
    @Test
    public void mustIgnoreQueueInOnExistingOffset() {
        List<Message> expected = Arrays.asList(
                new Message("http:id", "dst:id1", "payload1"),
                new Message("http:id", "dst:id2", "payload2"),
                new Message("http:id", "dst:id3", "payload3")
        );        
        fixture.queueIn("id", 0, expected.subList(0, 3));
        fixture.queueIn("id", 1, expected.subList(1, 3));
        fixture.queueIn("id", 2, expected.subList(2, 3));
        List<Message> actual = fixture.dequeueIn("id");

        assertMessagesEquals(expected, actual);
    }
    
    @Test
    public void mustFailIfQueueInOnNonExistingOffset() {
        List<Message> expected = Arrays.asList(
                new Message("http:id", "dst:id1", "payload1"),
                new Message("http:id", "dst:id2", "payload2"),
                new Message("http:id", "dst:id3", "payload3")
        );        
        fixture.queueIn("id", 0, expected.subList(0, 1));
        
        expectedException.expect(IllegalStateException.class);
        fixture.queueIn("id", 2, expected);
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
