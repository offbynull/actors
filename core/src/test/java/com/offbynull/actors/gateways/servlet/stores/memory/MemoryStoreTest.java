package com.offbynull.actors.gateways.servlet.stores.memory;

import com.offbynull.actors.shuttle.Message;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MemoryStoreTest {
    
    private Clock mockClock;
    private MemoryStore fixture;
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    @Before
    public void setUp() {
        mockClock = mock(Clock.class);
        when(mockClock.instant()).thenReturn(Instant.ofEpochMilli(0L));
        
        fixture = new MemoryStore("dst", 5, Duration.ofMillis(500L), mockClock);
    }

    @Test
    public void mustCreateQueueOnInitialWrite() {
        List<Message> inMsgs = Arrays.asList(
                new Message("src:src1", "dst:dst1", "payload1"),
                new Message("src:src2", "dst:dst2", "payload2"),
                new Message("src:src3", "dst:dst3", "payload3")
        );        
        fixture.write("fake", inMsgs);

        List<Message> outMsgs = fixture.read("fake");
        assertMessagesEquals(inMsgs, outMsgs);
    }

    @Test
    public void mustBeAbleToWriteInChunks() {
        List<Message> inMsgs1 = Arrays.asList(
                new Message("src:src1", "dst:dst1", "payload1"),
                new Message("src:src2", "dst:dst2", "payload2"),
                new Message("src:src3", "dst:dst3", "payload3")
        );        
        fixture.write("fake", inMsgs1);

        List<Message> inMsgs2 = Arrays.asList(
                new Message("src:src4", "dst:dst4", "payload4"),
                new Message("src:src5", "dst:dst5", "payload5")
        );        
        fixture.write("fake", inMsgs2);
        
        List<Message> outMsgs = fixture.read("fake");
        assertMessagesEquals(inMsgs1, outMsgs.subList(0, 3));
        assertMessagesEquals(inMsgs2, outMsgs.subList(3, 5));
    }

    @Test
    public void mustDiscardMessagesOnTimeout() {
        List<Message> inMsgs1 = Arrays.asList(
                new Message("src:src1", "dst:dst1", "payload1"),
                new Message("src:src2", "dst:dst2", "payload2"),
                new Message("src:src3", "dst:dst3", "payload3")
        );        
        fixture.write("fake", inMsgs1);

        List<Message> inMsgs2 = Arrays.asList(
                new Message("src:src4", "dst:dst4", "payload4"),
                new Message("src:src5", "dst:dst5", "payload5")
        );        
        fixture.write("fake", inMsgs2);
        
        when(mockClock.instant()).thenReturn(Instant.ofEpochMilli(500L));
        
        List<Message> outMsgs = fixture.read("fake");
        assertTrue(outMsgs.isEmpty());
    }
    
    @Test
    public void mustRefreshTimeoutIfWriteAgainBeforeTimeoutHit() {
        List<Message> inMsgs1 = Arrays.asList(
                new Message("src:src1", "dst:dst1", "payload1"),
                new Message("src:src2", "dst:dst2", "payload2"),
                new Message("src:src3", "dst:dst3", "payload3")
        );        
        fixture.write("fake", inMsgs1);

        when(mockClock.instant()).thenReturn(Instant.ofEpochMilli(400L));
        
        List<Message> inMsgs2 = Arrays.asList(
                new Message("src:src4", "dst:dst4", "payload4"),
                new Message("src:src5", "dst:dst5", "payload5")
        );        
        fixture.write("fake", inMsgs2);
        
        when(mockClock.instant()).thenReturn(Instant.ofEpochMilli(750L));
        
        List<Message> outMsgs = fixture.read("fake");
        assertMessagesEquals(inMsgs1, outMsgs.subList(0, 3));
        assertMessagesEquals(inMsgs2, outMsgs.subList(3, 5));
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
