package com.offbynull.actors.core.gateway.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import static com.offbynull.actors.core.common.DefaultAddresses.DEFAULT_WEB;
import com.offbynull.actors.core.shuttle.Message;
import java.util.Arrays;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class SystemToHttpBundleJsonSerializerTest {
    
    private Gson fixture;

    @Before
    public void before() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Message.class, new MessageJsonSerializer(DEFAULT_WEB));
        gsonBuilder.registerTypeAdapter(SystemToHttpBundle.class, new SystemToHttpBundleJsonSerializer(DEFAULT_WEB));
        fixture = gsonBuilder.serializeNulls().setPrettyPrinting().create();
    }

    @Test
    public void mustSerialize() {
        SystemToHttpBundle bundle = new SystemToHttpBundle(
                "custom_id",
                1,
                2,
                Arrays.asList(
                        new Message("direct", "web:custom_id", "HI!"),
                        new Message("actors:my_actor", "web:custom_id:sub_id", new TestObject(1))
                )
        );
        
        String actual = fixture.toJson(bundle);
        String expected = ""
                + "{\n"
                + "  \"httpAddressId\": \"custom_id\",\n"
                + "  \"systemToHttpOffset\": 1,\n"
                + "  \"httpToSystemOffset\": 2,\n"
                + "  \"messages\": [\n"
                + "    {\n"
                + "      \"source\": \"direct\",\n"
                + "      \"destination\": \"web:custom_id\",\n"
                + "      \"type\": \"java.lang.String\",\n"
                + "      \"content\": \"HI!\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"source\": \"actors:my_actor\",\n"
                + "      \"destination\": \"web:custom_id:sub_id\",\n"
                + "      \"type\": \"com.offbynull.actors.core.gateway.web.SystemToHttpBundleJsonSerializerTest$TestObject\",\n"
                + "      \"content\": {\n"
                + "        \"myInt\": 1\n"
                + "      }\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        assertEquals(expected, actual);
    }

    @Test
    public void mustSerializeEmptyMessages() {
        SystemToHttpBundle bundle = new SystemToHttpBundle(
                "custom_id",
                1,
                2,
                Arrays.<Message>asList()
        );
        
        String actual = fixture.toJson(bundle);
        String expected = ""
                + "{\n"
                + "  \"httpAddressId\": \"custom_id\",\n"
                + "  \"systemToHttpOffset\": 1,\n"
                + "  \"httpToSystemOffset\": 2,\n"
                + "  \"messages\": []\n"
                + "}";
        assertEquals(expected, actual);
    }

    @Test(expected=IllegalArgumentException.class)
    public void mustFailToSerializeOnBadPrefix() {
        SystemToHttpBundle bundle = new SystemToHttpBundle(
                "custom_id",
                1,
                2,
                Arrays.asList(
                        new Message("direct", "BAD_PREFIX:custom_id", "HI!"),
                        new Message("actors:my_actor", "web:custom_id:sub_id", new TestObject(1))
                )
        );
        
        fixture.toJson(bundle);
    }

    @Test(expected=IllegalArgumentException.class)
    public void mustFailToSerializeOnBadId() {
        SystemToHttpBundle bundle = new SystemToHttpBundle(
                "custom_id",
                1,
                2,
                Arrays.asList(
                        new Message("direct", "web:BAD_ID", "HI!"),
                        new Message("actors:my_actor", "web:custom_id:sub_id", new TestObject(1))
                )
        );
        
        fixture.toJson(bundle);
    }

    @Test(expected=IllegalArgumentException.class)
    public void mustFailToSerializeOnNoId() {
        SystemToHttpBundle bundle = new SystemToHttpBundle(
                "custom_id",
                1,
                2,
                Arrays.asList(
                        new Message("direct", "web", "HI!"),
                        new Message("actors:my_actor", "web:custom_id:sub_id", new TestObject(1))
                )
        );
        
        fixture.toJson(bundle);
    }

    @Test(expected=IllegalArgumentException.class)
    public void mustFailToSerializeOnNoPrefix() {
        SystemToHttpBundle bundle = new SystemToHttpBundle(
                "custom_id",
                1,
                2,
                Arrays.asList(
                        new Message("direct", "", "HI!"),
                        new Message("actors:my_actor", "web:custom_id:sub_id", new TestObject(1))
                )
        );
        
        fixture.toJson(bundle);
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
