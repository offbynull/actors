package com.offbynull.actors.core.gateway.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.offbynull.actors.core.shuttle.Message;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import static com.offbynull.actors.core.common.DefaultAddresses.DEFAULT_SERVLET;

public class HttpToSystemBundleJsonDeserializerTest {
    
    private Gson fixture;

    @Before
    public void before() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Message.class, new MessageJsonDeserializer(DEFAULT_SERVLET));
        gsonBuilder.registerTypeAdapter(HttpToSystemBundle.class, new HttpToSystemBundleJsonDeserializer(DEFAULT_SERVLET));
        fixture = gsonBuilder.serializeNulls().create();
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
                + "            source: \"servlet:custom_id\",\n"
                + "            destination: \"direct\",\n"
                + "            type: \"java.lang.String\",\n"
                + "            content: \"HI!\"\n"
                + "        },\n"
                + "        {\n"
                + "            source: \"servlet:custom_id:sub_id\",\n"
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
        
        assertEquals("servlet:custom_id", actual.getMessages().get(0).getSourceAddress().toString());
        assertEquals("direct", actual.getMessages().get(0).getDestinationAddress().toString());
        assertEquals("HI!", actual.getMessages().get(0).getMessage());
        
        assertEquals("servlet:custom_id:sub_id", actual.getMessages().get(1).getSourceAddress().toString());
        assertEquals("actors:my_actor", actual.getMessages().get(1).getDestinationAddress().toString());
        assertEquals(new TestObject(3), actual.getMessages().get(1).getMessage());
    }
    
    @Test
    public void mustDeserializeEmptyMessages() {
        String json = ""
                + "{\n"
                + "    httpAddressId: \"custom_id\",\n"
                + "    httpToSystemOffset: 1,\n"
                + "    systemToHttpOffset: 2,\n"
                + "    messages: []\n"
                + "}";
        HttpToSystemBundle actual = fixture.fromJson(json, HttpToSystemBundle.class);
        
        assertEquals("custom_id", actual.getHttpAddressId());
        assertEquals(1L, actual.getHttpToSystemOffset());
        assertEquals(2L, actual.getSystemToHttpOffset());
        
        assertEquals(0, actual.getMessages().size());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void mustFailToDeserializeOnBadPrefix() {
        String json = ""
                + "{\n"
                + "    httpAddressId: \"custom_id\",\n"
                + "    httpToSystemOffset: 1,\n"
                + "    systemToHttpOffset: 2,\n"
                + "    messages: [\n"
                + "        {\n"
                + "            source: \"BAD_PREFIX:custom_id\",\n"
                + "            destination: \"direct\",\n"
                + "            type: \"java.lang.String\",\n"
                + "            content: \"HI!\"\n"
                + "        },\n"
                + "        {\n"
                + "            source: \"servlet:custom_id:sub_id\",\n"
                + "            destination: \"actors:my_actor\",\n"
                + "            type: \"" + TestObject.class.getName() + "\",\n"
                + "            content: { myInt: 3 }\n"
                + "        }\n"
                + "    ]\n"
                + "}";
        fixture.fromJson(json, HttpToSystemBundle.class);
    }

    @Test(expected=IllegalArgumentException.class)
    public void mustFailToDeserializeOnNoPrefix() {
        String json = ""
                + "{\n"
                + "    httpAddressId: \"custom_id\",\n"
                + "    httpToSystemOffset: 1,\n"
                + "    systemToHttpOffset: 2,\n"
                + "    messages: [\n"
                + "        {\n"
                + "            source: \"\",\n"
                + "            destination: \"direct\",\n"
                + "            type: \"java.lang.String\",\n"
                + "            content: \"HI!\"\n"
                + "        },\n"
                + "        {\n"
                + "            source: \"servlet:custom_id:sub_id\",\n"
                + "            destination: \"actors:my_actor\",\n"
                + "            type: \"" + TestObject.class.getName() + "\",\n"
                + "            content: { myInt: 3 }\n"
                + "        }\n"
                + "    ]\n"
                + "}";
        fixture.fromJson(json, HttpToSystemBundle.class);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void mustFailToDeserializeOnBadId() {
        String json = ""
                + "{\n"
                + "    httpAddressId: \"custom_id\",\n"
                + "    httpToSystemOffset: 1,\n"
                + "    systemToHttpOffset: 2,\n"
                + "    messages: [\n"
                + "        {\n"
                + "            source: \"servlet:BAD_ID\",\n"
                + "            destination: \"direct\",\n"
                + "            type: \"java.lang.String\",\n"
                + "            content: \"HI!\"\n"
                + "        },\n"
                + "        {\n"
                + "            source: \"servlet:custom_id:sub_id\",\n"
                + "            destination: \"actors:my_actor\",\n"
                + "            type: \"" + TestObject.class.getName() + "\",\n"
                + "            content: { myInt: 3 }\n"
                + "        }\n"
                + "    ]\n"
                + "}";
        fixture.fromJson(json, HttpToSystemBundle.class);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void mustFailToDeserializeOnNoId() {
        String json = ""
                + "{\n"
                + "    httpAddressId: \"custom_id\",\n"
                + "    httpToSystemOffset: 1,\n"
                + "    systemToHttpOffset: 2,\n"
                + "    messages: [\n"
                + "        {\n"
                + "            source: \"servlet\",\n"
                + "            destination: \"direct\",\n"
                + "            type: \"java.lang.String\",\n"
                + "            content: \"HI!\"\n"
                + "        },\n"
                + "        {\n"
                + "            source: \"servlet:custom_id:sub_id\",\n"
                + "            destination: \"actors:my_actor\",\n"
                + "            type: \"" + TestObject.class.getName() + "\",\n"
                + "            content: { myInt: 3 }\n"
                + "        }\n"
                + "    ]\n"
                + "}";
        fixture.fromJson(json, HttpToSystemBundle.class);
    }

    @Test(expected=IllegalArgumentException.class)
    public void mustFailToDeserializeOnIncorrectType() {
        String json = ""
                + "{\n"
                + "    httpAddressId: \"custom_id\",\n"
                + "    httpToSystemOffset: 1,\n"
                + "    systemToHttpOffset: 2,\n"
                + "    messages: [\n"
                + "        {\n"
                + "            source: \"servlet:custom_id:sub_id\",\n"
                + "            destination: \"actors:my_actor\",\n"
                + "            type: \"" + TestObject.class.getName() + "\",\n"
                + "            content: \"HI!\"\n"
                + "        }\n"
                + "    ]\n"
                + "}";
        fixture.fromJson(json, HttpToSystemBundle.class);
    }

    @Test(expected=NullPointerException.class)
    public void mustFailToDeserializeOnNullOrMissingSourceAddress() {
        String json = ""
                + "{\n"
                + "    httpAddressId: \"custom_id\",\n"
                + "    httpToSystemOffset: 1,\n"
                + "    systemToHttpOffset: 2,\n"
                + "    messages: [\n"
                + "        {\n"
                + "            destination: \"actors:my_actor\",\n"
                + "            type: \"java.lang.String\",\n"
                + "            content: \"HI!\"\n"
                + "        }\n"
                + "    ]\n"
                + "}";
        fixture.fromJson(json, HttpToSystemBundle.class);
    }
    

    @Test(expected=NullPointerException.class)
    public void mustFailToDeserializeOnNullOrMissingDesteinationAddress() {
        String json = ""
                + "{\n"
                + "    httpAddressId: \"custom_id\",\n"
                + "    httpToSystemOffset: 1,\n"
                + "    systemToHttpOffset: 2,\n"
                + "    messages: [\n"
                + "        {\n"
                + "            source: \"servlet:custom_id\",\n"
                + "            type: \"java.lang.String\",\n"
                + "            content: \"HI!\"\n"
                + "        }\n"
                + "    ]\n"
                + "}";
        fixture.fromJson(json, HttpToSystemBundle.class);
    }
    

    @Test(expected=NullPointerException.class)
    public void mustFailToDeserializeOnNullOrMissingType() {
        String json = ""
                + "{\n"
                + "    httpAddressId: \"custom_id\",\n"
                + "    httpToSystemOffset: 1,\n"
                + "    systemToHttpOffset: 2,\n"
                + "    messages: [\n"
                + "        {\n"
                + "            source: \"servlet:custom_id\",\n"
                + "            destination: \"actors:my_actor\",\n"
                + "            content: \"HI!\"\n"
                + "        }\n"
                + "    ]\n"
                + "}";
        fixture.fromJson(json, HttpToSystemBundle.class);
    }
    

    @Test(expected=NullPointerException.class)
    public void mustFailToDeserializeOnNullOrMissingContent() {
        String json = ""
                + "{\n"
                + "    httpAddressId: \"custom_id\",\n"
                + "    httpToSystemOffset: 1,\n"
                + "    systemToHttpOffset: 2,\n"
                + "    messages: [\n"
                + "        {\n"
                + "            source: \"servlet:custom_id\",\n"
                + "            destination: \"actors:my_actor\",\n"
                + "            type: \"java.lang.String\"\n"
                + "        }\n"
                + "    ]\n"
                + "}";
        fixture.fromJson(json, HttpToSystemBundle.class);
    }
    
    private static final class TestObject {

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
