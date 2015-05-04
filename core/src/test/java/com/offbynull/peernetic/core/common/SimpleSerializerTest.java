package com.offbynull.peernetic.core.common;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class SimpleSerializerTest {
    
    private SimpleSerializer fixture;
    
    @Before
    public void setUp() {
        fixture = new SimpleSerializer();
    }

    @Test
    public void mustSerializeAndDeserializeBack() throws Exception {
        String obj = "object to serialize";
        
        byte[] data = fixture.serialize(obj);
        String deserializedObj = fixture.deserialize(data);
        
        assertEquals(obj, deserializedObj);
    }
    
}
