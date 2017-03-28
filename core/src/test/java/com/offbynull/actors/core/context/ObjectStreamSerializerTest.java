package com.offbynull.actors.core.context;

import com.offbynull.actors.core.shuttle.Address;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;
import java.io.Serializable;
import java.time.Instant;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.junit.Before;

public class ObjectStreamSerializerTest {

    public Serializer fixture;
    
    @Before
    public void before() {
        fixture = new ObjectStreamSerializer();
    }
    
    @Test
    public void mustSerializeAndDeserialize() {
        // Create actor
        Coroutine actor = (Coroutine & Serializable) cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            ctx.block("addr_to_block", false);
            
            TestOutputValueHolder.value = "first_val";
            cnt.suspend();
            TestOutputValueHolder.value = "second_val";
            cnt.suspend();
        };
        
        
        // Create source context for actor
        Address self = Address.fromString("a:b:c");
        CoroutineRunner actorRunner = new CoroutineRunner(actor);
        
        SourceContext ctxIn = new SourceContext(actorRunner, self);
        
        ctxIn.parent(new SourceContext(new CoroutineRunner((Coroutine & Serializable) cnt -> {}), Address.fromString("a:b")));
        ctxIn.time(Instant.EPOCH);
        ctxIn.source(Address.fromString("src"));
        ctxIn.destination(Address.fromString("dest"));
        ctxIn.in("testmsg");
        ctxIn.outs().add(new BatchedOutgoingMessage(Address.fromString("a"), Address.fromString("b"), "out1"));
        ctxIn.outs().add(new BatchedOutgoingMessage(Address.fromString("c"), Address.fromString("d"), "out2"));
        ctxIn.children().put("d1", new SourceContext(new CoroutineRunner((Coroutine & Serializable) cnt -> {}), Address.fromString("a:b:c:d1")));
        ctxIn.children().put("d2", new SourceContext(new CoroutineRunner((Coroutine & Serializable) cnt -> {}), Address.fromString("a:b:c:d2")));
        
        actorRunner.setContext(ctxIn);

        
        // Execute actor 1 step. TestValueHolder should have been inc by 1
        ctxIn.actorRunner().execute();
        assertEquals("first_val", TestOutputValueHolder.value);
        
        
        // Serialize the actor then unserialize the actor
        byte[] data = fixture.serialize(ctxIn);
        SourceContext ctxOut = fixture.unserialize(data);
        
        
        // Validate that the unserialized actor isn't the same object as the serialized actor
        assertNotEquals(ctxOut.actorRunner(), ctxIn.actorRunner());
        
        
        // Execute the UNSERIALIZED actor for another step. Since we serialized after we wrote the first value, when we execute the
        // unserialized version it should write the second value (it should be continuing from where we left off before serializing).
        ctxOut.actorRunner().execute();
        assertEquals("second_val", TestOutputValueHolder.value);
        
        
        // Make sure serialized and deserialized state is the same
        // assertEquals(ctxIn.parent(), ctxOut.parent());                     // can't check parents, SourceContext doesn't override equals
        assertNotNull(ctxIn.parent());
        assertNotNull(ctxOut.parent());
        assertEquals(ctxIn.time(), ctxOut.time());
        assertEquals(ctxIn.time(), ctxOut.time());
        assertEquals(ctxIn.source(), ctxOut.source());
        assertEquals(ctxIn.destination(), ctxOut.destination());
        assertEquals((Object) ctxIn.in(), (Object) ctxOut.in());
        assertEquals(ctxIn.outs(), ctxOut.outs());
        assertEquals(ctxIn.children().keySet(), ctxOut.children().keySet());  // SourceContext doesn't override equals, check keys only
    }
    
    // Why use this holder class? Because static fields are implicitly transient, so it won't get serialized + remains the same between
    // serialization/unserialization
    private static final class TestOutputValueHolder {
        private static String value;
    }
}
