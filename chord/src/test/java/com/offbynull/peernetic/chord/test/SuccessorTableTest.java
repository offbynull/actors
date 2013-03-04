package com.offbynull.peernetic.chord.test;

import com.google.common.collect.Lists;
import com.offbynull.peernetic.chord.Pointer;
import com.offbynull.peernetic.chord.SuccessorTable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

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
        Pointer basePtr = TestUtils.generatePointer(2, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        Pointer successorPtr = st.getSuccessor();
        
        assertEquals(basePtr, successorPtr);
        assertFalse(st.isEmpty());
    }

    public void testInitialEmpty1() {
        Pointer basePtr = TestUtils.generatePointer(2, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        st.moveToNextSucessor();
        assertNull(st.getSuccessor());
    }

    @Test
    public void testUpdate1() {
        Pointer basePtr = TestUtils.generatePointer(3, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        st.update(ptr1,
                Lists.newArrayList(ptr2, ptr3, ptr4, ptr5, ptr6, ptr7,
                basePtr));
        
        assertEquals(ptr1, st.getSuccessor());
        assertFalse(st.isEmpty());
        st.moveToNextSucessor();
        assertEquals(ptr2, st.getSuccessor());
        assertFalse(st.isEmpty());
        st.moveToNextSucessor();
        assertEquals(ptr3, st.getSuccessor());
        assertFalse(st.isEmpty());
        st.moveToNextSucessor();
        assertEquals(ptr4, st.getSuccessor());
        assertFalse(st.isEmpty());
        st.moveToNextSucessor();
        assertTrue(st.isEmpty());
    }

    @Test
    public void testUpdate2() {
        Pointer basePtr = TestUtils.generatePointer(3, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        
        st.update(ptr1,
                Lists.newArrayList(ptr2, basePtr, ptr1, ptr2, basePtr, ptr1,
                ptr2));
        
        assertEquals(ptr1, st.getSuccessor());
        assertFalse(st.isEmpty());
        st.moveToNextSucessor();
        assertEquals(ptr2, st.getSuccessor());
        assertFalse(st.isEmpty());
        st.moveToNextSucessor();
        assertEquals(basePtr, st.getSuccessor());
        assertFalse(st.isEmpty());
        st.moveToNextSucessor();
        assertTrue(st.isEmpty());
    }
    
    @Test
    public void testUpdateTrim1() {
        Pointer basePtr = TestUtils.generatePointer(3, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        st.update(ptr1,
                Lists.newArrayList(ptr2, ptr4, ptr5, ptr6, ptr7,
                basePtr));
        st.updateTrim(ptr3);
        
        assertEquals(ptr3, st.getSuccessor());
        assertFalse(st.isEmpty());
        st.moveToNextSucessor();
        assertEquals(ptr4, st.getSuccessor());
        assertFalse(st.isEmpty());
        st.moveToNextSucessor();
        assertEquals(ptr5, st.getSuccessor());
        assertFalse(st.isEmpty());
        st.moveToNextSucessor();
        assertTrue(st.isEmpty());
    }

    @Test
    public void testUpdateTrim2() {
        Pointer basePtr = TestUtils.generatePointer(3, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        st.update(ptr1,
                Lists.newArrayList(ptr2, ptr3, ptr4, ptr5, ptr6, ptr7,
                basePtr));
        st.updateTrim(ptr3);
        
        assertEquals(ptr3, st.getSuccessor());
        assertFalse(st.isEmpty());
        st.moveToNextSucessor();
        assertEquals(ptr4, st.getSuccessor());
        assertFalse(st.isEmpty());
        st.moveToNextSucessor();
        assertTrue(st.isEmpty());
    }

    @Test
    public void testUpdateTrim3() {
        Pointer basePtr = TestUtils.generatePointer(3, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        st.update(ptr1,
                Lists.newArrayList(ptr2, ptr3, ptr4, ptr5, ptr6, ptr7,
                basePtr));
        st.updateTrim(ptr7);
        
        assertEquals(ptr7, st.getSuccessor());
        assertFalse(st.isEmpty());
        st.moveToNextSucessor();
        assertTrue(st.isEmpty());
    }
}