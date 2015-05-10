package com.offbynull.peernetic.core.shuttles.simple;

import com.offbynull.peernetic.core.shuttle.Message;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class SimpleShuttleTest {
    
    private Bus bus;
    private SimpleShuttle fixture;
    
    @Before
    public void setUp() {
        bus = new Bus();
        fixture = new SimpleShuttle("test", bus);
    }
    
    @After
    public void tearDown() {
        bus.close();
    }

    @Test
    public void mustReadMessagesFromBusWhenInsertedInToShuttle() throws InterruptedException {
        fixture.send(Arrays.asList(new Message("src:fake1", "test:sub1", "hi1")));
        fixture.send(Arrays.asList(new Message("src:fake2", "test:sub2", "hi2")));
        
        List<Object> read = bus.pull();
        
        assertEquals(2, read.size());
        
        assertEquals("src:fake1", ((Message) read.get(0)).getSourceAddress());
        assertEquals("test:sub1", ((Message) read.get(0)).getDestinationAddress());
        assertEquals("hi1", ((Message) read.get(0)).getMessage());
        
        assertEquals("src:fake2", ((Message) read.get(1)).getSourceAddress());
        assertEquals("test:sub2", ((Message) read.get(1)).getDestinationAddress());
        assertEquals("hi2", ((Message) read.get(1)).getMessage());
    }

    @Test
    public void mustIgnoreMessagesBeingAddedThatHaveIncorrectDestinations() throws InterruptedException {
        fixture.send(Arrays.asList(new Message("src:fake1", "wrongdestination", "hi1")));
        fixture.send(Arrays.asList(new Message("src:fake2", "test:sub2", "hi2")));
        
        List<Object> read = bus.pull();
        
        assertEquals(1, read.size());
        
        assertEquals("src:fake2", ((Message) read.get(0)).getSourceAddress());
        assertEquals("test:sub2", ((Message) read.get(0)).getDestinationAddress());
        assertEquals("hi2", ((Message) read.get(0)).getMessage());
    }
    
}
