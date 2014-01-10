package com.offbynull.peernetic.overlay.chord;

import com.offbynull.peernetic.overlay.chord.core.SuccessorTable;
import com.google.common.collect.Lists;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.net.InetSocketAddress;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

public class SuccessorTableTest {
    
    public SuccessorTableTest() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testInitial1() {
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(2, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        Pointer<InetSocketAddress> successorPtr = st.getSuccessor();
        
        assertEquals(basePtr, successorPtr);
    }

    public void testInitialEmpty1() {
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(2, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        st.moveToNextSucessor();
        assertNull(st.getSuccessor());
    }

    @Test
    public void testUpdate1() {
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(3, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        Pointer<InetSocketAddress> ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer<InetSocketAddress> ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer<InetSocketAddress> ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer<InetSocketAddress> ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer<InetSocketAddress> ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer<InetSocketAddress> ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer<InetSocketAddress> ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        st.update(ptr1,
                Lists.newArrayList(ptr2, ptr3, ptr4, ptr5, ptr6, ptr7,
                basePtr));
        
        assertEquals(ptr1, st.getSuccessor());
        st.moveToNextSucessor();
        assertEquals(ptr2, st.getSuccessor());
        st.moveToNextSucessor();
        assertEquals(ptr3, st.getSuccessor());
        st.moveToNextSucessor();
        assertEquals(ptr4, st.getSuccessor());
    }

    @Test
    public void testUpdate2() {
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(3, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        Pointer<InetSocketAddress> ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer<InetSocketAddress> ptr2 = TestUtils.generatePointer(3, 0x02L);
        
        st.update(ptr1,
                Lists.newArrayList(ptr2, basePtr, ptr1, ptr2, basePtr, ptr1,
                ptr2));
        
        assertEquals(ptr1, st.getSuccessor());
        st.moveToNextSucessor();
        assertEquals(ptr2, st.getSuccessor());
    }

    @Test(expected = IllegalStateException.class)
    public void testUpdateFail1() {
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(3, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        Pointer<InetSocketAddress> ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer<InetSocketAddress> ptr2 = TestUtils.generatePointer(3, 0x02L);
        
        st.update(ptr1,
                Lists.newArrayList(ptr2, basePtr, ptr1, ptr2, basePtr, ptr1,
                ptr2));
        
        assertEquals(ptr1, st.getSuccessor());
        st.moveToNextSucessor();
        assertEquals(ptr2, st.getSuccessor());
        st.moveToNextSucessor();
    }
    
    @Test
    public void testUpdateTrim1() {
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(3, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        Pointer<InetSocketAddress> ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer<InetSocketAddress> ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer<InetSocketAddress> ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer<InetSocketAddress> ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer<InetSocketAddress> ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer<InetSocketAddress> ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer<InetSocketAddress> ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        st.update(ptr1,
                Lists.newArrayList(ptr2, ptr4, ptr5, ptr6, ptr7,
                basePtr));
        st.updateTrim(ptr3);
        
        assertEquals(ptr3, st.getSuccessor());
        st.moveToNextSucessor();
        assertEquals(ptr4, st.getSuccessor());
        st.moveToNextSucessor();
        assertEquals(ptr5, st.getSuccessor());
    }

    @Test
    public void testUpdateTrim2() {
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(3, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        Pointer<InetSocketAddress> ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer<InetSocketAddress> ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer<InetSocketAddress> ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer<InetSocketAddress> ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer<InetSocketAddress> ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer<InetSocketAddress> ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer<InetSocketAddress> ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        st.update(ptr1,
                Lists.newArrayList(ptr2, ptr3, ptr4, ptr5, ptr6, ptr7,
                basePtr));
        st.updateTrim(ptr3);
        
        assertEquals(ptr3, st.getSuccessor());
        st.moveToNextSucessor();
        assertEquals(ptr4, st.getSuccessor());
    }

    @Test
    public void testUpdateTrim3() {
        Pointer<InetSocketAddress> basePtr = TestUtils.generatePointer(3, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        Pointer<InetSocketAddress> ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer<InetSocketAddress> ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer<InetSocketAddress> ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer<InetSocketAddress> ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer<InetSocketAddress> ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer<InetSocketAddress> ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer<InetSocketAddress> ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        st.update(ptr1,
                Lists.newArrayList(ptr2, ptr3, ptr4, ptr5, ptr6, ptr7,
                basePtr));
        st.updateTrim(ptr7);
        
        assertEquals(ptr7, st.getSuccessor());
    }
}