package com.offbynull.peernetic.core.actor;

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
        List<BatchedOutgoingMessage> outgoingMsgsView = fixture.viewOutgoingMessages();
        
        fixture.addOutgoingMessage("test1", "1");
        fixture.addOutgoingMessage("src", "test2", "2");
        fixture.addOutgoingMessage("src", "test3", "3");
        
        assertEquals(3, outgoingMsgsView.size());
        
        assertNull(outgoingMsgsView.get(0).getSourceId());
        assertEquals("test1", outgoingMsgsView.get(0).getDestination());
        assertEquals("1", outgoingMsgsView.get(0).getMessage());
        
        assertEquals("src", outgoingMsgsView.get(1).getSourceId());
        assertEquals("test2", outgoingMsgsView.get(1).getDestination());
        assertEquals("2", outgoingMsgsView.get(1).getMessage());

        assertEquals("src", outgoingMsgsView.get(2).getSourceId());
        assertEquals("test3", outgoingMsgsView.get(2).getDestination());
        assertEquals("3", outgoingMsgsView.get(2).getMessage());
    }

    @Test
    public void mustDrainOutgoingMessages() {
        fixture.addOutgoingMessage("test1", "1");
        fixture.addOutgoingMessage("src", "test2", "2");
        fixture.addOutgoingMessage("src", "test3", "3");
        
        List<BatchedOutgoingMessage> outgoingMsgs = fixture.copyAndClearOutgoingMessages();
        
        assertTrue(fixture.copyAndClearOutgoingMessages().isEmpty());
        assertTrue(fixture.viewOutgoingMessages().isEmpty());
        
        assertEquals(3, outgoingMsgs.size());
        
        assertNull(outgoingMsgs.get(0).getSourceId());
        assertEquals("test1", outgoingMsgs.get(0).getDestination());
        assertEquals("1", outgoingMsgs.get(0).getMessage());
        
        assertEquals("src", outgoingMsgs.get(1).getSourceId());
        assertEquals("test2", outgoingMsgs.get(1).getDestination());
        assertEquals("2", outgoingMsgs.get(1).getMessage());

        assertEquals("src", outgoingMsgs.get(2).getSourceId());
        assertEquals("test3", outgoingMsgs.get(2).getDestination());
        assertEquals("3", outgoingMsgs.get(2).getMessage());
    }

    @Test
    public void mustConvertToNormalContext() {
        fixture.addOutgoingMessage("test1", "1");
        fixture.setDestination("dest");
        fixture.setIncomingMessage("msg");
        fixture.setSource("src");
        fixture.setSelf("self");
        fixture.setTime(Instant.MIN);
        
        Context normalContext = fixture.toNormalContext();
        
        List<BatchedOutgoingMessage> outgoingMsgsView = normalContext.viewOutgoingMessages();
        assertEquals(1, outgoingMsgsView.size());
        assertNull(outgoingMsgsView.get(0).getSourceId());
        assertEquals("test1", outgoingMsgsView.get(0).getDestination());
        assertEquals("1", outgoingMsgsView.get(0).getMessage());
        
        assertEquals("dest", normalContext.getDestination());
        assertEquals("msg", normalContext.getIncomingMessage());
        assertEquals("src", normalContext.getSource());
        assertEquals("self", normalContext.getSelf());
        assertEquals(Instant.MIN, normalContext.getTime());
    }
    
}
