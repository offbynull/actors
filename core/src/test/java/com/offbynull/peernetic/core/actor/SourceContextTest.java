package com.offbynull.peernetic.core.actor;

import com.offbynull.peernetic.core.shuttle.Address;
import java.time.Instant;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class SourceContextTest {
    
    private SourceContext fixture;
    
    @Before
    public void setUp() {
        fixture = new SourceContext();
    }

    @Test
    public void mustQueueUpOutgoingMessages() {
        List<BatchedOutgoingMessage> outgoingMsgsView = fixture.viewOuts();
        
        fixture.out(Address.fromString("test1"), "1");
        fixture.out(Address.fromString("src"), Address.fromString("test2"), "2");
        fixture.out(Address.fromString("src"), Address.fromString("test3"), "3");
        
        assertEquals(3, outgoingMsgsView.size());
        
        assertEquals(Address.of(), outgoingMsgsView.get(0).getSource());
        assertEquals(Address.of("test1"), outgoingMsgsView.get(0).getDestination());
        assertEquals("1", outgoingMsgsView.get(0).getMessage());
        
        assertEquals(Address.of("src"), outgoingMsgsView.get(1).getSource());
        assertEquals(Address.of("test2"), outgoingMsgsView.get(1).getDestination());
        assertEquals("2", outgoingMsgsView.get(1).getMessage());

        assertEquals(Address.of("src"), outgoingMsgsView.get(2).getSource());
        assertEquals(Address.of("test3"), outgoingMsgsView.get(2).getDestination());
        assertEquals("3", outgoingMsgsView.get(2).getMessage());
    }

    @Test
    public void mustDrainOutgoingMessages() {
        fixture.out(Address.fromString("test1"), "1");
        fixture.out(Address.fromString("src"), Address.fromString("test2"), "2");
        fixture.out(Address.fromString("src"), Address.fromString("test3"), "3");
        
        List<BatchedOutgoingMessage> outgoingMsgs = fixture.copyAndClearOutgoingMessages();
        
        assertTrue(fixture.copyAndClearOutgoingMessages().isEmpty());
        assertTrue(fixture.viewOuts().isEmpty());
        
        assertEquals(3, outgoingMsgs.size());
        
        assertEquals(Address.of(), outgoingMsgs.get(0).getSource());
        assertEquals(Address.of("test1"), outgoingMsgs.get(0).getDestination());
        assertEquals("1", outgoingMsgs.get(0).getMessage());
        
        assertEquals(Address.of("src"), outgoingMsgs.get(1).getSource());
        assertEquals(Address.of("test2"), outgoingMsgs.get(1).getDestination());
        assertEquals("2", outgoingMsgs.get(1).getMessage());

        assertEquals(Address.of("src"), outgoingMsgs.get(2).getSource());
        assertEquals(Address.of("test3"), outgoingMsgs.get(2).getDestination());
        assertEquals("3", outgoingMsgs.get(2).getMessage());
    }

    @Test
    public void mustConvertToNormalContext() {
        fixture.out(Address.fromString("test1"), "1");
        fixture.setDestination(Address.of("dest"));
        fixture.setIncomingMessage(Address.of("msg"));
        fixture.setSource(Address.of("src"));
        fixture.setSelf(Address.of("self"));
        fixture.setTime(Instant.MIN);
        
        Context normalContext = fixture.toNormalContext();
        
        List<BatchedOutgoingMessage> outgoingMsgsView = normalContext.viewOuts();
        assertEquals(1, outgoingMsgsView.size());
        assertEquals(Address.of(), outgoingMsgsView.get(0).getSource());
        assertEquals(Address.of("test1"), outgoingMsgsView.get(0).getDestination());
        assertEquals("1", outgoingMsgsView.get(0).getMessage());
        
        assertEquals(Address.of("dest"), normalContext.destination());
        assertEquals(Address.of("msg"), normalContext.in());
        assertEquals(Address.of("src"), normalContext.source());
        assertEquals(Address.of("self"), normalContext.self());
        assertEquals(Instant.MIN, normalContext.time());
    }
    
}
