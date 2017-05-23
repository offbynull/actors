package com.offbynull.actors.core.gateway.servlet;

import com.offbynull.actors.core.gateway.servlet.MessageCache.MessageBlock;
import com.offbynull.actors.core.shuttle.Message;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableLong;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class InMemoryMessageCacheTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    @Test
    public void mustAddAndReadSystemToHttpMessages() {
        MessageCache fixture = new InMemoryMessageCache(60000L);
        
        fixture.keepAlive("client_id");
        
        fixture.systemToHttpAppend("client_id", new Message("src1", "dst1", "msg1"));
        MessageBlock mb1 = fixture.systemToHttpRead("client_id");
        assertEquals(0, mb1.getStartSequenceOffset());
        assertEquals(1, mb1.getMessages().size());
        assertEquals("src1", mb1.getMessages().get(0).getSourceAddress().toString());
        assertEquals("dst1", mb1.getMessages().get(0).getDestinationAddress().toString());
        assertEquals("msg1", mb1.getMessages().get(0).getMessage());
        
        fixture.systemToHttpAppend("client_id",
                new Message("src2", "dst2", "msg2"),
                new Message("src3", "dst3", "msg3"),
                new Message("src4", "dst4", "msg4"));
        MessageBlock mb2 = fixture.systemToHttpRead("client_id");
        assertEquals(0, mb2.getStartSequenceOffset());
        assertEquals(4, mb2.getMessages().size());        
        assertEquals("src1", mb2.getMessages().get(0).getSourceAddress().toString());
        assertEquals("dst1", mb2.getMessages().get(0).getDestinationAddress().toString());
        assertEquals("msg1", mb2.getMessages().get(0).getMessage());
        assertEquals("src2", mb2.getMessages().get(1).getSourceAddress().toString());
        assertEquals("dst2", mb2.getMessages().get(1).getDestinationAddress().toString());
        assertEquals("msg2", mb2.getMessages().get(1).getMessage());
        assertEquals("src3", mb2.getMessages().get(2).getSourceAddress().toString());
        assertEquals("dst3", mb2.getMessages().get(2).getDestinationAddress().toString());
        assertEquals("msg3", mb2.getMessages().get(2).getMessage());
        assertEquals("src4", mb2.getMessages().get(3).getSourceAddress().toString());
        assertEquals("dst4", mb2.getMessages().get(3).getDestinationAddress().toString());
        assertEquals("msg4", mb2.getMessages().get(3).getMessage());
    }
    
    @Test
    public void mustFailToAddSystemToHttpMessagesIfIdNotTracked() {
        MessageCache fixture = new InMemoryMessageCache(60000L);
        
        expectedException.expect(IllegalArgumentException.class);
        fixture.systemToHttpAppend("client_id", new Message("src1", "dst1", "msg1"));
    }
    
    @Test
    public void mustFailToAddSystemToHttpMessagesIfIdNotTrackedViaTimeout() {
        MutableLong fakeTime = new MutableLong(0L);
        MessageCache fixture = new InMemoryMessageCache(1L, () -> {
            fakeTime.increment();
            return fakeTime.longValue();
        });
        
        fixture.keepAlive("client_id");
        
        expectedException.expect(IllegalArgumentException.class);
        fixture.systemToHttpAppend("client_id", new Message("src1", "dst1", "msg1"));
    }
}
