package com.offbynull.peernetic.core.shuttles.test;

import com.offbynull.peernetic.core.shuttle.Message;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;


public class CaptureShuttleTest {
    
    private CaptureShuttle fixture;
    
    @Before
    public void setUp() {
        fixture = new CaptureShuttle("test");
    }

    @Test
    public void mustReadMessagesFromBusWhenInsertedInToShuttle() throws InterruptedException {
        fixture.send(Arrays.asList(new Message("src:fake1", "test:sub1", "hi1")));
        fixture.send(Arrays.asList(new Message("src:fake2", "test:sub2", "hi2")));
        
        List<Message> read = fixture.drainMessages();
        
        assertEquals(2, read.size());
        
        assertEquals("src:fake1", read.get(0).getSourceAddress());
        assertEquals("test:sub1", read.get(0).getDestinationAddress());
        assertEquals("hi1", read.get(0).getMessage());
        
        assertEquals("src:fake2", read.get(1).getSourceAddress());
        assertEquals("test:sub2", read.get(1).getDestinationAddress());
        assertEquals("hi2", read.get(1).getMessage());
    }

    @Test
    public void mustIgnoreMessagesBeingAddedThatHaveIncorrectDestinations() throws InterruptedException {
        fixture.send(Arrays.asList(new Message("src:fake1", "wrongdestination", "hi1")));
        fixture.send(Arrays.asList(new Message("src:fake2", "test:sub2", "hi2")));
        
        Message msg = fixture.takeNextMessage();
        
        assertEquals("src:fake2", msg.getSourceAddress());
        assertEquals("test:sub2", msg.getDestinationAddress());
        assertEquals("hi2", msg.getMessage());

        assertEquals(0, fixture.drainMessages().size());
    }
    
}
