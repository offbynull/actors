package com.offbynull.peernetic.network.actors.udpsimulator;

import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.shuttle.Address;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Collection;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class SimpleLineTest {
    
    private SimpleLine fixture;
    
    @Before
    public void setUp() {
        fixture = new SimpleLine(0L, new SimpleSerializer());
    }

    @Test
    public void mustProperlyProcessOutgoing() {
        SimpleSerializer serializer = new SimpleSerializer();
        Collection<TransitMessage> tms = fixture.processOutgoing(
                Instant.MIN,
                new DepartMessage(
                        "hi",
                        Address.of(),
                        Address.of("dst")));
        assertEquals(1, tms.size());
        assertEquals(Address.of(), tms.iterator().next().getSourceId());
        assertEquals(Address.of("dst"), tms.iterator().next().getDestinationAddress());
        assertEquals(ByteBuffer.wrap(serializer.serialize("hi")), tms.iterator().next().getMessage());
    }

    @Test
    public void mustProperlyProcessIncoming() {
        SimpleSerializer serializer = new SimpleSerializer();
        Collection<TransitMessage> tms = fixture.processIncoming(
                Instant.MIN,
                new DepartMessage(
                        ByteBuffer.wrap(serializer.serialize("hi")),
                        Address.of(),
                        Address.of("dst")));
        assertEquals(1, tms.size());
        assertEquals(Address.of(), tms.iterator().next().getSourceId());
        assertEquals(Address.of("dst"), tms.iterator().next().getDestinationAddress());
        assertEquals("hi", tms.iterator().next().getMessage());
    }
    
}
