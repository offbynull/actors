package com.offbynull.actors.core.gateway.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import static com.offbynull.actors.core.common.DefaultAddresses.DEFAULT_WEB;
import com.offbynull.actors.core.shuttle.Message;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class HttpToSystemBundleJsonDeserializerTest {
    
    private Gson fixture;

    @Before
    public void before() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Message.class, new MessageJsonDeserializer(DEFAULT_WEB));
        gsonBuilder.registerTypeAdapter(HttpToSystemBundle.class, new HttpToSystemBundleJsonDeserializer(DEFAULT_WEB));
        fixture = gsonBuilder.create();
    }
    
    @Test
    public void mustDeserialize() {
        String json = ""
                + "{\n"
                + "    httpAddressId: \"custom_id\",\n"
                + "    httpToSystemOffset: 1,\n"
                + "    systemToHttpOffset: 2,\n"
                + "    messages: [\n"
                + "        {\n"
                + "            source: \"web:custom_id\",\n"
                + "            destination: \"direct\",\n"
                + "            type: \"java.lang.String\",\n"
                + "            content: \"HI!\"\n"
                + "        },\n"
                + "        {\n"
                + "            source: \"web:custom_id:sub_id\",\n"
                + "            destination: \"actors:my_actor\",\n"
                + "            type: \"" + TestObject.class.getName() + "\",\n"
                + "            content: { myInt: 3 }\n"
                + "        }\n"
                + "    ]\n"
                + "}";
        HttpToSystemBundle actual = fixture.fromJson(json, HttpToSystemBundle.class);
        
        assertEquals("custom_id", actual.getHttpAddressId());
        assertEquals(1L, actual.getHttpToSystemOffset());
        assertEquals(2L, actual.getSystemToHttpOffset());
        
        assertEquals(2, actual.getMessages().size());
        
        assertEquals("web:custom_id", actual.getMessages().get(0).getSourceAddress().toString());
        assertEquals("direct", actual.getMessages().get(0).getDestinationAddress().toString());
        assertEquals("HI!", actual.getMessages().get(0).getMessage());
        
        assertEquals("web:custom_id:sub_id", actual.getMessages().get(1).getSourceAddress().toString());
        assertEquals("actors:my_actor", actual.getMessages().get(1).getDestinationAddress().toString());
        assertEquals(new TestObject(3), actual.getMessages().get(1).getMessage());
    }
    
    
    public static final class TestObject {

        public TestObject(int myInt) {
            this.myInt = myInt;
        }
        
        int myInt;

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + this.myInt;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TestObject other = (TestObject) obj;
            if (this.myInt != other.myInt) {
                return false;
            }
            return true;
        }
        
    }
}
