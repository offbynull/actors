package com.offbynull.peernetic.chord.test;

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
    public void testIsDead1() {
        Pointer basePtr = TestUtils.generatePointer(2, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        assertFalse(cs.isDead());
    }

    @Test
    public void testIsDead2() {
        Pointer basePtr = TestUtils.generatePointer(2, 0x00L);
        ChordState cs = new ChordState(basePtr);
        
        cs.shiftSuccessor();
        
        assertTrue(cs.isDead());
    }
}