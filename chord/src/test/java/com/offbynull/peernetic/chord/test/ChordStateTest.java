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
    public void testSetSuccessor1() {
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
        assertEquals(ptr1, cs.getPredecessor());
    }

    @Test
    public void testSetSuccessor2() {
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
        assertEquals(ptr7, cs.getPredecessor());
    }

    @Test
    public void testSetSuccessor3() {
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
        assertEquals(ptr7, cs.getPredecessor());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSetSuccessor4() {
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
    public void testSetSuccessor5() {
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
    public void testSetSuccessor6() {
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
    public void testShiftSuccessor1() {
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
        assertEquals(ptr7, cs.getPredecessor());
    }
    
    @Test
    public void testShiftSuccessor2() {
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
        cs.setPredecessor(ptr1);
        cs.shiftSuccessor();
        
        assertEquals(ptr7, cs.getFinger(0));
        assertEquals(ptr7, cs.getFinger(1));
        assertEquals(ptr7, cs.getFinger(2));
        assertEquals(ptr7, cs.getSuccessor());
        assertEquals(ptr7, cs.getPredecessor());
    }
    
    @Test
    public void testShiftSuccessor3() {
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
        cs.setPredecessor(ptr2);
        cs.shiftSuccessor();
        
        assertEquals(ptr7, cs.getFinger(0));
        assertEquals(ptr7, cs.getFinger(1));
        assertEquals(ptr7, cs.getFinger(2));
        assertEquals(ptr7, cs.getSuccessor());
        assertEquals(ptr7, cs.getPredecessor());
    }
    
    @Test
    public void testShiftSuccessor4() {
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
        assertEquals(ptr1, cs.getPredecessor());
        
        cs.shiftSuccessor();
        
        assertEquals(ptr7, cs.getFinger(0));
        assertEquals(ptr7, cs.getFinger(1));
        assertEquals(ptr7, cs.getFinger(2));
        assertEquals(ptr7, cs.getSuccessor());
        assertEquals(ptr7, cs.getPredecessor());
        
        boolean expHit = false;
        try {
            cs.shiftSuccessor();
        } catch (IllegalStateException ise) {
            expHit = true;
        }
        assertTrue(expHit);
    }
    
    @Test(expected = IllegalStateException.class)
    public void testShiftSuccessor5() {
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
        assertEquals(ptr1, cs.getPredecessor());
        
        cs.shiftSuccessor();
    }
    
    @Test(expected = IllegalStateException.class)
    public void testShiftSuccessor6() {
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
    
    @Test
    public void testSetPredecessor1() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.setPredecessor(ptr1);
        
        assertEquals(ptr1, cs.getFinger(0));
        assertEquals(basePtr, cs.getFinger(1));
        assertEquals(basePtr, cs.getFinger(2));
        assertEquals(ptr1, cs.getSuccessor());
        assertEquals(ptr1, cs.getPredecessor());
    }

    @Test
    public void testSetPredecessor2() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.putFinger(ptr1);
        cs.setPredecessor(ptr7);
        
        assertEquals(ptr1, cs.getFinger(0));
        assertEquals(ptr7, cs.getFinger(1));
        assertEquals(ptr7, cs.getFinger(2));
        assertEquals(ptr1, cs.getSuccessor());
        assertEquals(ptr7, cs.getPredecessor());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetPredecessor3() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.putFinger(ptr7);
        cs.setPredecessor(ptr1);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSetPredecessor4() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.putFinger(ptr4);
        cs.setPredecessor(ptr7);
        cs.setPredecessor(ptr6);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSetPredecessor5() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.setPredecessor(basePtr);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetPredecessor6() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.putFinger(ptr7);
        cs.setPredecessor(basePtr);
    }

    @Test
    public void testRemovePredecessor1() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.putFinger(ptr2);
        cs.setPredecessor(ptr7);
        cs.removePredecessor();
        
        // at this point, ptr7 will be shoved in to finger table but not
        // removed
        
        
        assertEquals(ptr2, cs.getFinger(0));
        assertEquals(ptr2, cs.getFinger(1));
        assertEquals(basePtr, cs.getFinger(2));
        assertEquals(ptr2, cs.getSuccessor());
        assertNull(cs.getPredecessor());
    }

    @Test
    public void testRemovePredecessor2() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.putFinger(ptr2);
        cs.removePredecessor(); // removing pred removes max non-base finger...
                                // in this case max non-base finger is also
                                // finger[0] aka successor, so everything is
                                // trashed
        
        assertEquals(basePtr, cs.getFinger(0));
        assertEquals(basePtr, cs.getFinger(1));
        assertEquals(basePtr, cs.getFinger(2));
        assertEquals(basePtr, cs.getSuccessor());
        assertNull(cs.getPredecessor());
    }
    
    @Test
    public void testPutFinger1() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.putFinger(ptr1);
        
        assertEquals(ptr1, cs.getFinger(0));
        assertEquals(basePtr, cs.getFinger(1));
        assertEquals(basePtr, cs.getFinger(2));
        assertEquals(ptr1, cs.getSuccessor());
        assertEquals(ptr1, cs.getPredecessor());
    }
    
    @Test
    public void testPutFinger2() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.putFinger(ptr1);
        cs.putFinger(ptr2);
        cs.putFinger(ptr4);
        
        assertEquals(ptr1, cs.getFinger(0));
        assertEquals(ptr2, cs.getFinger(1));
        assertEquals(ptr4, cs.getFinger(2));
        assertEquals(ptr1, cs.getSuccessor());
        assertEquals(ptr4, cs.getPredecessor());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutFinger3() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.putFinger(basePtr);
    }
    
    @Test
    public void testRemoveFinger1() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.putFinger(ptr1);
        cs.putFinger(ptr2);
        cs.putFinger(ptr4);
        cs.removeFinger(ptr2);
        
        assertEquals(ptr1, cs.getFinger(0));
        assertEquals(ptr4, cs.getFinger(1));
        assertEquals(ptr4, cs.getFinger(2));
        assertEquals(ptr1, cs.getSuccessor());
        assertEquals(ptr4, cs.getPredecessor());
    }
    
    @Test
    public void testRemoveFinger2() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.putFinger(ptr1);
        cs.putFinger(ptr2);
        cs.putFinger(ptr4);
        cs.setPredecessor(ptr4);
        cs.removeFinger(ptr1);
        
        assertEquals(ptr2, cs.getFinger(0));
        assertEquals(ptr2, cs.getFinger(1));
        assertEquals(ptr4, cs.getFinger(2));
        assertEquals(ptr2, cs.getSuccessor());
        assertEquals(ptr4, cs.getPredecessor());
    }
    
    @Test
    public void testRemoveFinger3() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.putFinger(ptr1);
        cs.putFinger(ptr2);
        cs.putFinger(ptr4);
        cs.setPredecessor(ptr4);
        cs.removeFinger(ptr4);
        
        assertEquals(ptr1, cs.getFinger(0));
        assertEquals(ptr2, cs.getFinger(1));
        assertEquals(basePtr, cs.getFinger(2));
        assertEquals(ptr1, cs.getSuccessor());
        assertEquals(ptr2, cs.getPredecessor());
    }

    @Test
    public void testRemoveFinger4() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.putFinger(ptr1);
        cs.putFinger(ptr2);
        cs.putFinger(ptr4);
        cs.setPredecessor(ptr4);
        cs.removeFinger(ptr5);
        
        assertEquals(ptr1, cs.getFinger(0));
        assertEquals(ptr2, cs.getFinger(1));
        assertEquals(ptr4, cs.getFinger(2));
        assertEquals(ptr1, cs.getSuccessor());
        assertEquals(ptr4, cs.getPredecessor());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRemoveFinger5() {
        Pointer basePtr = TestUtils.generatePointer(3, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        Pointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        Pointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        Pointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        Pointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        Pointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        Pointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        Pointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        cs.putFinger(ptr1);
        cs.putFinger(ptr2);
        cs.putFinger(ptr4);
        cs.setPredecessor(ptr4);
        cs.removeFinger(basePtr);
    }
}