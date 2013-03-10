package com.offbynull.peernetic.chord.test;

import com.google.common.collect.Lists;
import com.offbynull.peernetic.chord.ChordState;
import com.offbynull.peernetic.chord.Pointer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChordStateTest {
    
    public ChordStateTest() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testInitialState() {
        Pointer basePtr = TestUtils.generatePointer(2, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(2, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(2, 0x02L);
        
        assertEquals(cs.getBase(), basePtr);
        assertEquals(cs.getExpectedFingerId(0), ptr1.getId());
        assertEquals(cs.getFinger(0), basePtr);
        assertEquals(cs.getExpectedFingerId(1), ptr2.getId());
        assertEquals(cs.getFinger(1), basePtr);
        assertEquals(cs.getPredecessor(), null);
        assertEquals(cs.getSuccessor(), basePtr);
    }

    @Test
    public void testSetSuccessorSync1() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.setSuccessor(ptr1, Lists.<Pointer>newArrayList());
        
        assertEquals(ptr1, cs.getFinger(0));
        assertEquals(basePtr, cs.getFinger(1));
        assertEquals(basePtr, cs.getFinger(2));
        
        assertEquals(ptr1, cs.getSuccessor());
        assertNull(cs.getPredecessor());
    }

    @Test
    public void testSetSuccessorSync2() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.setSuccessor(ptr1, Lists.<Pointer>newArrayList());
        cs.setSuccessor(ptr7, Lists.<Pointer>newArrayList());
        
        assertEquals(ptr7, cs.getFinger(0));
        assertEquals(ptr7, cs.getFinger(1));
        assertEquals(ptr7, cs.getFinger(2));
        
        assertEquals(ptr7, cs.getSuccessor());
        assertNull(cs.getPredecessor());
    }

    @Test
    public void testSetSuccessorSync3() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.setSuccessor(ptr7, Lists.<Pointer>newArrayList());
        cs.setSuccessor(ptr1, Lists.<Pointer>newArrayList());
        
        assertEquals(ptr1, cs.getFinger(0));
        assertEquals(ptr7, cs.getFinger(1));
        assertEquals(ptr7, cs.getFinger(2));
        
        assertEquals(ptr1, cs.getSuccessor());
        assertNull(cs.getPredecessor());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSetSuccessorFail1() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.setSuccessor(ptr7, Lists.<Pointer>newArrayList(ptr1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetSuccessorFail2() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.setSuccessor(ptr7, Lists.<Pointer>newArrayList(ptr7));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSetSuccessorFail3() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.setSuccessor(ptr1, Lists.<Pointer>newArrayList(ptr5, ptr5));
    }
    
    @Test
    public void testShiftSuccessorSync1() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.setSuccessor(ptr1, Lists.<Pointer>newArrayList(ptr7));
        cs.shiftSuccessor();
        
        assertEquals(ptr7, cs.getFinger(0));
        assertEquals(ptr7, cs.getFinger(1));
        assertEquals(ptr7, cs.getFinger(2));
        assertEquals(ptr7, cs.getSuccessor());
        assertNull(cs.getPredecessor());
    }
    
    @Test
    public void testShiftSuccessorSync2() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.setSuccessor(ptr1, Lists.<Pointer>newArrayList(ptr7, basePtr, ptr1));

        assertEquals(ptr1, cs.getFinger(0));
        assertEquals(basePtr, cs.getFinger(1));
        assertEquals(basePtr, cs.getFinger(2));
        assertEquals(ptr1, cs.getSuccessor());
        assertNull(cs.getPredecessor());
        
        cs.shiftSuccessor();
        
        assertEquals(ptr7, cs.getFinger(0));
        assertEquals(ptr7, cs.getFinger(1));
        assertEquals(ptr7, cs.getFinger(2));
        assertEquals(ptr7, cs.getSuccessor());
        assertNull(cs.getPredecessor());
        
        boolean expHit = false;
        try {
            cs.shiftSuccessor();
        } catch (IllegalStateException ise) {
            expHit = true;
        }
        assertTrue(expHit);
    }
    
    @Test(expected = IllegalStateException.class)
    public void testShiftSuccessorFail1() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.setSuccessor(ptr1, Lists.<Pointer>newArrayList());
        
        assertEquals(ptr1, cs.getFinger(0));
        assertEquals(basePtr, cs.getFinger(1));
        assertEquals(basePtr, cs.getFinger(2));
        assertEquals(ptr1, cs.getSuccessor());
        assertNull(cs.getPredecessor());
        
        cs.shiftSuccessor();
    }
    
    @Test(expected = IllegalStateException.class)
    public void testShiftSuccessorFail2() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.setSuccessor(ptr1, Lists.<Pointer>newArrayList(ptr7));
        cs.shiftSuccessor();
        cs.shiftSuccessor();
    }
}