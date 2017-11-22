package com.offbynull.actors.core.gateways.actor;

import com.offbynull.actors.core.gateways.actor.Context;
import com.offbynull.actors.core.gateways.actor.Context.BatchedOutgoingMessageCommand;
import com.offbynull.actors.core.shuttle.Address;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class ContextTest {
    
    private Context fixture;
    
    @Before
    public void setUp() {
        fixture = new Context(Address.fromString("self"));
    }

    @Test
    public void mustQueueUpOutgoingMessages() {
        List<BatchedOutgoingMessageCommand> outgoingMsgsView = fixture.viewOuts();
        
        fixture.out("test1", "1");
        fixture.out("self", "test2", "2");
        fixture.out("self:a", "test3", "3");
        
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
        fixture.out("test1", "1");
        fixture.out("self", "test2", "2");
        fixture.out("self:a", "test3", "3");
        
        List<BatchedOutgoingMessageCommand> outgoingMsgs = fixture.copyAndClearOutgoingMessages();
        
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
    
}
