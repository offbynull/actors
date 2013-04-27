package com.offbynull.peernetic.p2putils.tests;

import com.offbynull.peernetic.p2ptools.identification.BitLimitedId;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedPointer;
import com.offbynull.peernetic.p2ptools.overlay.chord.FingerTable;
import com.offbynull.peernetic.p2ptools.overlay.chord.FingerTable.RouteResult;
import com.offbynull.peernetic.p2ptools.overlay.chord.FingerTable.RouteResult.ResultType;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class RingRoutingTableTest {
    
    public RingRoutingTableTest() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test(expected=IllegalArgumentException.class)
    public void testPut1() { 
        BitLimitedPointer basePtr = TestUtils.generatePointer(2, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        ft.put(basePtr);
    }
    
    @Test
    public void testPut2() { 
        BitLimitedPointer basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        BitLimitedPointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        
        ft.put(ptr4);
        
        BitLimitedPointer entry;
        
        entry = ft.get(0);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr4, entry);
        entry = ft.get(2);
        assertEquals(ptr4, entry);
    }

    @Test
    public void testPut3() { 
        BitLimitedPointer basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        BitLimitedPointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        BitLimitedPointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        
        ft.put(ptr4);
        ft.put(ptr5);
        
        BitLimitedPointer entry;
        
        entry = ft.get(0);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr4, entry);
        entry = ft.get(2);
        assertEquals(ptr5, entry);
    }
    
    @Test
    public void testReplace1() { 
        BitLimitedPointer basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        BitLimitedPointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        
        ft.replace(ptr4);
        
        BitLimitedPointer entry;
        
        entry = ft.get(0);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr4, entry);
        entry = ft.get(2);
        assertEquals(ptr4, entry);
    }
    
    @Test
    public void testReplace2() { 
        BitLimitedPointer basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        BitLimitedPointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        BitLimitedPointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        
        ft.put(ptr5);
        ft.replace(ptr4);
        
        BitLimitedPointer entry;
        
        entry = ft.get(0);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr4, entry);
        entry = ft.get(2);
        assertEquals(ptr4, entry);
    }

    @Test
    public void testReplace3() { 
        BitLimitedPointer basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        BitLimitedPointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        BitLimitedPointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        BitLimitedPointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        
        ft.replace(ptr1);
        ft.replace(ptr5);
        ft.replace(ptr4);
        
        BitLimitedPointer entry;
        
        entry = ft.get(0);
        assertEquals(ptr1, entry);
        entry = ft.get(1);
        assertEquals(ptr4, entry);
        entry = ft.get(2);
        assertEquals(ptr4, entry);
    }
    
    @Test
    public void testEmptyFingerTable() {
        BitLimitedPointer basePtr = TestUtils.generatePointer(2, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        BitLimitedId id1 = TestUtils.generateId(2, 0x00L);
        BitLimitedId id2 = TestUtils.generateId(2, 0x01L);
        BitLimitedId id3 = TestUtils.generateId(2, 0x02L);
        BitLimitedId id4 = TestUtils.generateId(2, 0x03L);
        
        RouteResult res;
        
        res = ft.route(id1);
        assertEquals(basePtr.getId(), res.getPointer().getId());
        assertEquals(ResultType.SELF, res.getResultType());
        res = ft.route(id2);
        assertEquals(basePtr.getId(), res.getPointer().getId());
        assertEquals(ResultType.SELF, res.getResultType());
        res = ft.route(id3);
        assertEquals(basePtr.getId(), res.getPointer().getId());
        assertEquals(ResultType.SELF, res.getResultType());
        res = ft.route(id4);
        assertEquals(basePtr.getId(), res.getPointer().getId());
        assertEquals(ResultType.SELF, res.getResultType());
    }

    @Test
    public void testPartialFingerTable() {
        BitLimitedPointer basePtr = TestUtils.generatePointer(2, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        BitLimitedPointer firstNeighbour = TestUtils.generatePointer(2, 0x01L);
        BitLimitedPointer secondNeighbour = TestUtils.generatePointer(2, 0x02L);
        ft.put(secondNeighbour);
        BitLimitedPointer thirdNeighbour = TestUtils.generatePointer(2, 0x03L);
        
        RouteResult res;
        
        res = ft.route(basePtr.getId());
        assertEquals(basePtr.getId(), res.getPointer().getId());
        assertEquals(ResultType.SELF, res.getResultType());
        // 1st is less than successor (2nd), so it'll give back 2nd
        res = ft.route(firstNeighbour.getId());
        assertEquals(secondNeighbour.getId(), res.getPointer().getId());
        assertEquals(ResultType.FOUND, res.getResultType());
        // 2nd is successor, so it'll give back 2nd
        res = ft.route(secondNeighbour.getId());
        assertEquals(secondNeighbour.getId(), res.getPointer().getId());
        assertEquals(ResultType.FOUND, res.getResultType());
        // 3rd is past successor, so it'll give back the last finger table
        // entry that's < 3rd (not <=, just <), which is 2nd 
        res = ft.route(thirdNeighbour.getId());
        assertEquals(secondNeighbour.getId(), res.getPointer().getId());
        assertEquals(ResultType.CLOSEST_PREDECESSOR, res.getResultType());
    }
    
    @Test
    public void testFullFingerTable() {
        BitLimitedPointer basePtr = TestUtils.generatePointer(2, 0x00L);
        FingerTable ft = new FingerTable(basePtr);

        BitLimitedPointer firstNeighbour = TestUtils.generatePointer(2, 0x01L);
        ft.put(firstNeighbour);
        BitLimitedPointer secondNeighbour = TestUtils.generatePointer(2, 0x02L);
        ft.put(secondNeighbour);
        BitLimitedPointer thirdNeighbour = TestUtils.generatePointer(2, 0x03L);
        ft.put(thirdNeighbour);
        
        RouteResult res;
        
        res = ft.route(basePtr.getId());
        assertEquals(basePtr.getId(), res.getPointer().getId());
        assertEquals(ResultType.SELF, res.getResultType());
        // 1st is successor (1st), so it'll give back 1st
        res = ft.route(firstNeighbour.getId());
        assertEquals(firstNeighbour.getId(), res.getPointer().getId());
        assertEquals(ResultType.FOUND, res.getResultType());
        // 2nd should be directly in the finger table (just after successor --
        // value 10b), but since 2nd is past the successor, this will give us
        // back the entry just before it, which should be the succesor (1st)
        res = ft.route(secondNeighbour.getId());
        assertEquals(firstNeighbour.getId(), res.getPointer().getId());
        assertEquals(ResultType.CLOSEST_PREDECESSOR, res.getResultType());
        // 3rd should be directly in the finger table (just after successor --
        // actual id 11b, placed in finger table index 2 for desired id of 10b),
        // but since it is past the successor, this will give us back the finger
        // table entry just before it, which should be the succesor (1st)
        res = ft.route(thirdNeighbour.getId());
        assertEquals(firstNeighbour.getId(), res.getPointer().getId());
        assertEquals(ResultType.CLOSEST_PREDECESSOR, res.getResultType());
    }
    
    @Test
    public void testRemove1() { 
        BitLimitedPointer basePtr = TestUtils.generatePointer(2, 0x02L);
        FingerTable ft = new FingerTable(basePtr);
        BitLimitedPointer ptr2 = TestUtils.generatePointer(2, 0x00L);
        BitLimitedPointer ptr3 = TestUtils.generatePointer(2, 0x01L);

        ft.put(ptr2);
        ft.remove(ptr3);

        // nothing should be removed
        BitLimitedPointer entry;
        
        entry = ft.get(1);
        assertEquals(ptr2, entry);
        entry = ft.get(0);
        assertEquals(ptr2, entry);
    }
    
    @Test
    public void testRemove2() { 
        BitLimitedPointer basePtr = TestUtils.generatePointer(2, 0x02L);
        FingerTable ft = new FingerTable(basePtr);
        BitLimitedPointer ptr2 = TestUtils.generatePointer(2, 0x00L);
        
        ft.put(ptr2);
        ft.remove(ptr2);

        // table should be reverted
        BitLimitedPointer entry;
        
        entry = ft.get(0);
        assertEquals(basePtr, entry);
        entry = ft.get(1);
        assertEquals(basePtr, entry);
    }

    @Test
    public void testRemove3() {
        BitLimitedPointer basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        BitLimitedPointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        BitLimitedPointer ptr4 = TestUtils.generatePointer(3, 0x04L);

        ft.put(ptr2);
        ft.put(ptr4);
        ft.remove(ptr4);
        
        // preceding table entry should NOT be replaced
        BitLimitedPointer entry;
        
        entry = ft.get(2);
        assertEquals(basePtr, entry);
        entry = ft.get(1);
        assertEquals(ptr2, entry);
        entry = ft.get(0);
        assertEquals(ptr2, entry);
    }

    @Test
    public void testClearBefore1() { 
        BitLimitedPointer basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        BitLimitedPointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        BitLimitedPointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        
        ft.put(ptr2);
        ft.put(ptr4);
        int cleared = ft.clearBefore(ptr4.getId());

        assertEquals(1, cleared);
        // should be a clear finger table
        BitLimitedPointer entry;
        
        entry = ft.get(2);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr4, entry);
        entry = ft.get(0);
        assertEquals(ptr4, entry);
    }

    @Test
    public void testClearBefore2() { 
        BitLimitedPointer basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        BitLimitedPointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        BitLimitedPointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        BitLimitedPointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        
        ft.put(ptr2);
        ft.put(ptr4);
        int cleared = ft.clearBefore(ptr3.getId());

        assertEquals(1, cleared);
        
        // should be a clear finger table
        BitLimitedPointer entry;
        
        entry = ft.get(2);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr4, entry);
        entry = ft.get(0);
        assertEquals(ptr4, entry);
    }

    @Test
    public void testClearBefore3() { 
        BitLimitedPointer basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        BitLimitedPointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        BitLimitedPointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        BitLimitedPointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        
        ft.put(ptr2);
        ft.put(ptr4);
        int cleared = ft.clearBefore(ptr1.getId());
        
        assertEquals(0, cleared);

        // should be a clear finger table
        BitLimitedPointer entry;
        
        entry = ft.get(2);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr2, entry);
        entry = ft.get(0);
        assertEquals(ptr2, entry);
    }

    @Test
    public void testClearAfter1() { 
        BitLimitedPointer basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        BitLimitedPointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        BitLimitedPointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        
        ft.put(ptr2);
        ft.put(ptr4);
        int cleared = ft.clearAfter(basePtr.getId());
        
        assertEquals(3, cleared);

        // should be a clear finger table
        BitLimitedPointer entry;
        
        entry = ft.get(2);
        assertEquals(basePtr, entry);
        entry = ft.get(1);
        assertEquals(basePtr, entry);
        entry = ft.get(0);
        assertEquals(basePtr, entry);
    }

    @Test
    public void testClearAfter2() { 
        BitLimitedPointer basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        BitLimitedPointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        BitLimitedPointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        BitLimitedPointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        
        ft.put(ptr2);
        ft.put(ptr4);
        int cleared = ft.clearAfter(ptr3.getId());
        
        assertEquals(1, cleared);

        // should be a clear finger table
        BitLimitedPointer entry;
        
        entry = ft.get(2);
        assertEquals(basePtr, entry);
        entry = ft.get(1);
        assertEquals(ptr2, entry);
        entry = ft.get(0);
        assertEquals(ptr2, entry);
    }

    @Test
    public void testClearAfter3() { 
        BitLimitedPointer basePtr = TestUtils.generatePointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        BitLimitedPointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        BitLimitedPointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        BitLimitedPointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        
        ft.put(ptr2);
        ft.put(ptr4);
        int cleared = ft.clearAfter(ptr5.getId());
        
        assertEquals(0, cleared);

        // should be a clear finger table
        BitLimitedPointer entry;
        
        entry = ft.get(2);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr2, entry);
        entry = ft.get(0);
        assertEquals(ptr2, entry);
    }
}
