package com.offbynull.actors.core.stores.memory;

import com.offbynull.actors.core.stores.memory.BestEffortSerializer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import org.apache.commons.collections4.list.UnmodifiableList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class BestEffortSerializerTest {
    
    @Rule
    public ExpectedException expected = ExpectedException.none();
    
    private BestEffortSerializer fixture = new BestEffortSerializer();

    @Test
    public void mustSerializeAndDeserializeComplexObject() {
        TestClass writeObj = new TestClass("ffff");
        TestClass readObj;
        
        byte[] data = fixture.serialize(writeObj);
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        readObj = fixture.deserialize(data);

        assertTrue(readObj != null);
        assertNotEquals(readObj, writeObj);
        
        assertTrue(readObj.innerClass != null);
        assertNotEquals(readObj.innerClass, writeObj.innerClass);
        
        assertTrue(readObj.anonymousClass != null);
        assertNotEquals(readObj.anonymousClass, writeObj.anonymousClass);
        
        assertArrayEquals(writeObj.x, readObj.x, 0.0f);
        assertEquals(writeObj.b, readObj.b);
        assertEquals(writeObj.i, readObj.i);
        assertNotEquals(writeObj.r, readObj.r);
        assertEquals(writeObj.c, readObj.c);
        assertEquals(writeObj.a, readObj.a);
        assertEquals(writeObj.nsc.size(), readObj.nsc.size());
        assertTrue(readObj.nsc.get(0) != null && readObj.nsc.get(0) != writeObj.nsc.get(0));
        assertTrue(readObj.nsc.get(1) != null && readObj.nsc.get(1) != writeObj.nsc.get(1));
        assertTrue(readObj.nsc.get(2) != null && readObj.nsc.get(2) != writeObj.nsc.get(2));
        assertEquals(writeObj.s, readObj.s);
        assertEquals(writeObj.nullmap, readObj.nullmap);
    }

    @Test
    public void mustFailToDeserializeWithNonSerializableLambda() {
        TestClass writeObj = new TestClass("ffff");
        writeObj.c = x -> {};
        byte[] data = fixture.serialize(writeObj);
        
        expected.expect(IllegalStateException.class);
        expected.expectMessage("Lambda");
        fixture.deserialize(data);
    }
    
    
    private static final class TestClass {

        private transient TestClass self = this;
        private transient MyInnerClass innerClass = new MyInnerClass(99L);
        private transient Object anonymousClass;
        private transient float[] x = { 0.0f, 1.0f, 2.0f };
        private transient long b = 2;
        private transient Integer i = 111;
        private Random r = new Random(0);
        private Consumer<String> c = null; //x -> {}
        private final int a = 1;
        private final List<Object> nsc = UnmodifiableList.unmodifiableList(new LinkedList<>(Arrays.asList(
                new NonSerializableClass(),
                new NonSerializableClass(),
                new NonSerializableClass()
        )));
        private final String marker = "HEYHEYHEYHEYHEYHEYHEYHEYHEYHEYHEYHEYHEYHEYHEYHEYHEYHEYHEYHEYHEYHEYHEYHEYHEYHEYHEYHEYHEYHEYHEYHEYHEY";
        private final String s;
        private final Map<String, Integer> nullmap = null;

        public TestClass(String s) {
            this.s = s;
            anonymousClass = new MyInnerInterface() {
                String x = "anonClass " + s;
                
                @Override
                public void test() {
                    System.out.println(x);
                }
            };
        }

        @Override
        public String toString() {
            return "NonSerializableWithSerializable{" + "a=" + a + ", nsc=" + nsc + ", s=" + s + '}';
        }
        
        public class MyInnerClass {
            long x;

            public MyInnerClass(long x) {
                this.x = x;
            }
            
        }
        
        public interface MyInnerInterface {
            void test();
        }
    }

    private static final class NonSerializableClass {

        private final double d = Math.random();

        @Override
        public String toString() {
            return "NonSerializableClass{" + "d=" + d + '}';
        }

    }
}
