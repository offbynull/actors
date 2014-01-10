package com.offbynull.peernetic.overlay.chord;

import com.offbynull.peernetic.overlay.chord.core.ChordState;
import com.google.common.collect.Lists;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.net.InetSocketAddress;
import java.util.List;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

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
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(2, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        assertEquals(cs.getBase(), basePtr);
        assertEquals(cs.getExpectedFingerId(0), ptrList.get(1).getId());
        assertEquals(cs.getFinger(0), basePtr);
        assertEquals(cs.getExpectedFingerId(1), ptrList.get(2).getId());
        assertEquals(cs.getFinger(1), basePtr);
        assertEquals(cs.getPredecessor(), null);
        assertEquals(cs.getSuccessor(), basePtr);
    }

    @Test
    public void testSetSuccessor1() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.setSuccessor(ptrList.get(1), Lists.<Pointer<InetSocketAddress>>newArrayList());
        
        assertEquals(ptrList.get(1), cs.getFinger(0));
        assertEquals(basePtr, cs.getFinger(1));
        assertEquals(basePtr, cs.getFinger(2));        
        assertEquals(ptrList.get(1), cs.getSuccessor());
        assertEquals(ptrList.get(1), cs.getPredecessor());
    }

    @Test
    public void testSetSuccessor2() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.setSuccessor(ptrList.get(1), Lists.<Pointer<InetSocketAddress>>newArrayList());
        cs.setSuccessor(ptrList.get(7), Lists.<Pointer<InetSocketAddress>>newArrayList());
        
        assertEquals(ptrList.get(7), cs.getFinger(0));
        assertEquals(ptrList.get(7), cs.getFinger(1));
        assertEquals(ptrList.get(7), cs.getFinger(2));
        
        assertEquals(ptrList.get(7), cs.getSuccessor());
        assertEquals(ptrList.get(7), cs.getPredecessor());
    }

    @Test
    public void testSetSuccessor3() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.setSuccessor(ptrList.get(7), Lists.<Pointer<InetSocketAddress>>newArrayList());
        cs.setSuccessor(ptrList.get(1), Lists.<Pointer<InetSocketAddress>>newArrayList());
        
        assertEquals(ptrList.get(1), cs.getFinger(0));
        assertEquals(ptrList.get(7), cs.getFinger(1));
        assertEquals(ptrList.get(7), cs.getFinger(2));
        
        assertEquals(ptrList.get(1), cs.getSuccessor());
        assertEquals(ptrList.get(7), cs.getPredecessor());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSetSuccessor4() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.setSuccessor(ptrList.get(7), Lists.<Pointer<InetSocketAddress>>newArrayList(ptrList.get(1)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetSuccessor5() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.setSuccessor(ptrList.get(7), Lists.<Pointer<InetSocketAddress>>newArrayList(ptrList.get(7)));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSetSuccessor6() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.setSuccessor(ptrList.get(1), Lists.<Pointer<InetSocketAddress>>newArrayList(ptrList.get(5), ptrList.get(5)));
    }
    
    @Test
    public void testShiftSuccessor1() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.setSuccessor(ptrList.get(1), Lists.<Pointer<InetSocketAddress>>newArrayList(ptrList.get(7)));
        cs.shiftSuccessor();
        
        assertEquals(ptrList.get(7), cs.getFinger(0));
        assertEquals(ptrList.get(7), cs.getFinger(1));
        assertEquals(ptrList.get(7), cs.getFinger(2));
        assertEquals(ptrList.get(7), cs.getSuccessor());
        assertEquals(ptrList.get(7), cs.getPredecessor());
    }
    
    @Test
    public void testShiftSuccessor2() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.setSuccessor(ptrList.get(1), Lists.<Pointer<InetSocketAddress>>newArrayList(ptrList.get(7)));
        cs.setPredecessor(ptrList.get(1));
        cs.shiftSuccessor();
        
        assertEquals(ptrList.get(7), cs.getFinger(0));
        assertEquals(ptrList.get(7), cs.getFinger(1));
        assertEquals(ptrList.get(7), cs.getFinger(2));
        assertEquals(ptrList.get(7), cs.getSuccessor());
        assertEquals(ptrList.get(7), cs.getPredecessor());
    }
    
    @Test
    public void testShiftSuccessor3() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.setSuccessor(ptrList.get(1), Lists.<Pointer<InetSocketAddress>>newArrayList(ptrList.get(7)));
        cs.setPredecessor(ptrList.get(2));
        cs.shiftSuccessor();
        
        assertEquals(ptrList.get(7), cs.getFinger(0));
        assertEquals(ptrList.get(7), cs.getFinger(1));
        assertEquals(ptrList.get(7), cs.getFinger(2));
        assertEquals(ptrList.get(7), cs.getSuccessor());
        assertEquals(ptrList.get(7), cs.getPredecessor());
    }
    
    @Test
    public void testShiftSuccessor4() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.setSuccessor(ptrList.get(1), Lists.<Pointer<InetSocketAddress>>newArrayList(ptrList.get(7), basePtr, ptrList.get(1)));

        assertEquals(ptrList.get(1), cs.getFinger(0));
        assertEquals(basePtr, cs.getFinger(1));
        assertEquals(basePtr, cs.getFinger(2));
        assertEquals(ptrList.get(1), cs.getSuccessor());
        assertEquals(ptrList.get(1), cs.getPredecessor());
        
        cs.shiftSuccessor();
        
        assertEquals(ptrList.get(7), cs.getFinger(0));
        assertEquals(ptrList.get(7), cs.getFinger(1));
        assertEquals(ptrList.get(7), cs.getFinger(2));
        assertEquals(ptrList.get(7), cs.getSuccessor());
        assertEquals(ptrList.get(7), cs.getPredecessor());
        
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
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.setSuccessor(ptrList.get(1), Lists.<Pointer<InetSocketAddress>>newArrayList());
        
        assertEquals(ptrList.get(1), cs.getFinger(0));
        assertEquals(basePtr, cs.getFinger(1));
        assertEquals(basePtr, cs.getFinger(2));
        assertEquals(ptrList.get(1), cs.getSuccessor());
        assertEquals(ptrList.get(1), cs.getPredecessor());
        
        cs.shiftSuccessor();
    }
    
    @Test(expected = IllegalStateException.class)
    public void testShiftSuccessor6() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.setSuccessor(ptrList.get(1), Lists.<Pointer<InetSocketAddress>>newArrayList(ptrList.get(7)));
        cs.shiftSuccessor();
        cs.shiftSuccessor();
    }
    
    @Test
    public void testSetPredecessor1() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.setPredecessor(ptrList.get(1));
        
        assertEquals(ptrList.get(1), cs.getFinger(0));
        assertEquals(basePtr, cs.getFinger(1));
        assertEquals(basePtr, cs.getFinger(2));
        assertEquals(ptrList.get(1), cs.getSuccessor());
        assertEquals(ptrList.get(1), cs.getPredecessor());
    }

    @Test
    public void testSetPredecessor2() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.putFinger(ptrList.get(1));
        cs.setPredecessor(ptrList.get(7));
        
        assertEquals(ptrList.get(1), cs.getFinger(0));
        assertEquals(ptrList.get(7), cs.getFinger(1));
        assertEquals(ptrList.get(7), cs.getFinger(2));
        assertEquals(ptrList.get(1), cs.getSuccessor());
        assertEquals(ptrList.get(7), cs.getPredecessor());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetPredecessor3() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.putFinger(ptrList.get(7));
        cs.setPredecessor(ptrList.get(1));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSetPredecessor4() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.putFinger(ptrList.get(4));
        cs.setPredecessor(ptrList.get(7));
        cs.setPredecessor(ptrList.get(6));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSetPredecessor5() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.setPredecessor(basePtr);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetPredecessor6() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.putFinger(ptrList.get(7));
        cs.setPredecessor(basePtr);
    }

    @Test
    public void testRemovePredecessor1() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.putFinger(ptrList.get(2));
        cs.setPredecessor(ptrList.get(7));
        cs.removePredecessor();
        
        // at this point, ptrList.get(7) will be shoved in to finger table but not
        // removed
        
        
        assertEquals(ptrList.get(2), cs.getFinger(0));
        assertEquals(ptrList.get(2), cs.getFinger(1));
        assertEquals(basePtr, cs.getFinger(2));
        assertEquals(ptrList.get(2), cs.getSuccessor());
        assertNull(cs.getPredecessor());
    }

    @Test
    public void testRemovePredecessor2() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.putFinger(ptrList.get(2));
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
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.putFinger(ptrList.get(1));
        
        assertEquals(ptrList.get(1), cs.getFinger(0));
        assertEquals(basePtr, cs.getFinger(1));
        assertEquals(basePtr, cs.getFinger(2));
        assertEquals(ptrList.get(1), cs.getSuccessor());
        assertEquals(ptrList.get(1), cs.getPredecessor());
    }
    
    @Test
    public void testPutFinger2() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.putFinger(ptrList.get(1));
        cs.putFinger(ptrList.get(2));
        cs.putFinger(ptrList.get(4));
        
        assertEquals(ptrList.get(1), cs.getFinger(0));
        assertEquals(ptrList.get(2), cs.getFinger(1));
        assertEquals(ptrList.get(4), cs.getFinger(2));
        assertEquals(ptrList.get(1), cs.getSuccessor());
        assertEquals(ptrList.get(4), cs.getPredecessor());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutFinger3() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.putFinger(basePtr);
    }
    
    @Test
    public void testRemoveFinger1() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.putFinger(ptrList.get(1));
        cs.putFinger(ptrList.get(2));
        cs.putFinger(ptrList.get(4));
        cs.removeFinger(ptrList.get(2));
        
        assertEquals(ptrList.get(1), cs.getFinger(0));
        assertEquals(ptrList.get(4), cs.getFinger(1));
        assertEquals(ptrList.get(4), cs.getFinger(2));
        assertEquals(ptrList.get(1), cs.getSuccessor());
        assertEquals(ptrList.get(4), cs.getPredecessor());
    }
    
    @Test
    public void testRemoveFinger2() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.putFinger(ptrList.get(1));
        cs.putFinger(ptrList.get(2));
        cs.putFinger(ptrList.get(4));
        cs.setPredecessor(ptrList.get(4));
        cs.removeFinger(ptrList.get(1));
        
        assertEquals(ptrList.get(2), cs.getFinger(0));
        assertEquals(ptrList.get(2), cs.getFinger(1));
        assertEquals(ptrList.get(4), cs.getFinger(2));
        assertEquals(ptrList.get(2), cs.getSuccessor());
        assertEquals(ptrList.get(4), cs.getPredecessor());
    }
    
    @Test
    public void testRemoveFinger3() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.putFinger(ptrList.get(1));
        cs.putFinger(ptrList.get(2));
        cs.putFinger(ptrList.get(4));
        cs.setPredecessor(ptrList.get(4));
        cs.removeFinger(ptrList.get(4));
        
        assertEquals(ptrList.get(1), cs.getFinger(0));
        assertEquals(ptrList.get(2), cs.getFinger(1));
        assertEquals(basePtr, cs.getFinger(2));
        assertEquals(ptrList.get(1), cs.getSuccessor());
        assertEquals(ptrList.get(2), cs.getPredecessor());
    }

    @Test
    public void testRemoveFinger4() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.putFinger(ptrList.get(1));
        cs.putFinger(ptrList.get(2));
        cs.putFinger(ptrList.get(4));
        cs.setPredecessor(ptrList.get(4));
        cs.removeFinger(ptrList.get(5));
        
        assertEquals(ptrList.get(1), cs.getFinger(0));
        assertEquals(ptrList.get(2), cs.getFinger(1));
        assertEquals(ptrList.get(4), cs.getFinger(2));
        assertEquals(ptrList.get(1), cs.getSuccessor());
        assertEquals(ptrList.get(4), cs.getPredecessor());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRemoveFinger5() {
        List<Pointer<InetSocketAddress>> ptrList = TestUtils.generatePointers(3, 0x00L);
        
        Pointer<InetSocketAddress> basePtr = ptrList.get(0);
        ChordState cs = new ChordState(basePtr);
        
        cs.putFinger(ptrList.get(1));
        cs.putFinger(ptrList.get(2));
        cs.putFinger(ptrList.get(4));
        cs.setPredecessor(ptrList.get(4));
        cs.removeFinger(basePtr);
    }
}