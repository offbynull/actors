package com.offbynull.peernetic.core.actor;

import com.offbynull.peernetic.core.shuttle.Address;
import java.time.Instant;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class SourceContextTest {
    
    private SourceContext fixture;
    
    @Before
    public void setUp() {
        fixture = new SourceContext();
        fixture.setSelf(Address.fromString("self"));
    }

    @Test
    public void mustQueueUpOutgoingMessages() {
        List<BatchedOutgoingMessage> outgoingMsgsView = fixture.viewOuts();
        
        fixture.out(Address.fromString("test1"), "1");
        fixture.out(Address.fromString("self"), Address.fromString("test2"), "2");
        fixture.out(Address.fromString("self:a"), Address.fromString("test3"), "3");
        
        assertEquals(3, outgoingMsgsView.size());
        
        assertEquals(Address.fromString("self"), outgoingMsgsView.get(0).getSource());
        assertEquals(Address.fromString("test1"), outgoingMsgsView.get(0).getDestination());
        assertEquals("1", outgoingMsgsView.get(0).getMessage());
        
        assertEquals(Address.fromString("self"), outgoingMsgsView.get(1).getSource());
        assertEquals(Address.fromString("test2"), outgoingMsgsView.get(1).getDestination());
        assertEquals("2", outgoingMsgsView.get(1).getMessage());

        assertEquals(Address.fromString("self:a"), outgoingMsgsView.get(2).getSource());
        assertEquals(Address.fromString("test3"), outgoingMsgsView.get(2).getDestination());
        assertEquals("3", outgoingMsgsView.get(2).getMessage());
    }

    @Test
    public void mustDrainOutgoingMessages() {
        fixture.out(Address.fromString("test1"), "1");
        fixture.out(Address.fromString("self"), Address.fromString("test2"), "2");
        fixture.out(Address.fromString("self:a"), Address.fromString("test3"), "3");
        
        List<BatchedOutgoingMessage> outgoingMsgs = fixture.copyAndClearOutgoingMessages();
        
        assertTrue(fixture.copyAndClearOutgoingMessages().isEmpty());
        assertTrue(fixture.viewOuts().isEmpty());
        
        assertEquals(3, outgoingMsgs.size());
        
        assertEquals(Address.fromString("self"), outgoingMsgs.get(0).getSource());
        assertEquals(Address.fromString("test1"), outgoingMsgs.get(0).getDestination());
        assertEquals("1", outgoingMsgs.get(0).getMessage());
        
        assertEquals(Address.fromString("self"), outgoingMsgs.get(1).getSource());
        assertEquals(Address.fromString("test2"), outgoingMsgs.get(1).getDestination());
        assertEquals("2", outgoingMsgs.get(1).getMessage());

        assertEquals(Address.fromString("self:a"), outgoingMsgs.get(2).getSource());
        assertEquals(Address.fromString("test3"), outgoingMsgs.get(2).getDestination());
        assertEquals("3", outgoingMsgs.get(2).getMessage());
    }

    @Test
    public void mustConvertToNormalContext() {
        fixture.out(Address.fromString("test1"), "1");
        fixture.setSelf(Address.fromString("self"));
        fixture.setSource(Address.fromString("self:src"));
        fixture.setDestination(Address.fromString("dest"));
        fixture.setIncomingMessage("msg");
        fixture.setTime(Instant.MIN);
        
        Context normalContext = fixture.toNormalContext();
        
        List<BatchedOutgoingMessage> outgoingMsgsView = normalContext.viewOuts();
        assertEquals(1, outgoingMsgsView.size());
        assertEquals(Address.fromString("self"), outgoingMsgsView.get(0).getSource());
        assertEquals(Address.fromString("test1"), outgoingMsgsView.get(0).getDestination());
        assertEquals("1", outgoingMsgsView.get(0).getMessage());
        
        assertEquals(Address.fromString("dest"), normalContext.destination());
        assertEquals("msg", normalContext.in());
        assertEquals(Address.fromString("self:src"), normalContext.source());
        assertEquals(Address.fromString("self"), normalContext.self());
        assertEquals(Instant.MIN, normalContext.time());
    }
    
}
