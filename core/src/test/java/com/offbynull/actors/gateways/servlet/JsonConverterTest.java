package com.offbynull.actors.gateways.servlet;

import com.offbynull.actors.shuttle.Message;
import java.util.Arrays;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class JsonConverterTest {
    
    private JsonConverter fixture;
    
    @Before
    public void setUp() {
        fixture = new JsonConverter();
    }
    
    @Test
    public void mustConvertJsonStringToRequestBlock() {
        String json = 
                "{\n"
                + "  id: 'hi',\n"
                + "  messages: [\n"
                + "   {\n"
                + "     source='a:a1',\n"
                + "     destination='b:b1',\n"
                + "     type='java.lang.String',\n"
                + "     data='payload1'\n"
                + "   },\n"
                + "   {\n"
                + "     source='a:a2',\n"
                + "     destination='b:b2',\n"
                + "     type='java.lang.String',\n"
                + "     data='payload2'\n"
                + "   }\n"
                + "  ]\n"
                + "}";
        
        RequestBlock rb = fixture.fromJson(json);
        
        assertEquals("hi", rb.getId());
        assertEquals(2, rb.getMessages().size());

        assertEquals("a:a1", rb.getMessages().get(0).getSourceAddress().toString());
        assertEquals("b:b1", rb.getMessages().get(0).getDestinationAddress().toString());
        assertEquals("payload1", rb.getMessages().get(0).getMessage());

        assertEquals("a:a2", rb.getMessages().get(1).getSourceAddress().toString());
        assertEquals("b:b2", rb.getMessages().get(1).getDestinationAddress().toString());
        assertEquals("payload2", rb.getMessages().get(1).getMessage());
    }

    @Test
    public void mustGenerateResponseBlockToJsonString() {
        ResponseBlock rb = new ResponseBlock(Arrays.asList(
                new Message("a:a1", "b:b1", "payload1"),
                new Message("a:a2", "b:b2", "payload2")
        ));
        String json = fixture.toJson(rb);
        
        assertEquals("{\"messages\":[{\"source\":\"a:a1\",\"destination\":\"b:b1\",\"type\":\"java.lang.String\",\"data\":\"payload1\"},{\"source\":\"a:a2\",\"destination\":\"b:b2\",\"type\":\"java.lang.String\",\"data\":\"payload2\"}]}", json);
    }
    
}
