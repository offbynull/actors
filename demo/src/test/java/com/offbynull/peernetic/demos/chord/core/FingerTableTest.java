package com.offbynull.peernetic.demos.chord.core;

import com.offbynull.peernetic.demos.chord.core.Pointer;
import com.offbynull.peernetic.demos.chord.core.FingerTable;
import com.offbynull.peernetic.demos.chord.core.InternalPointer;
import com.offbynull.peernetic.demos.chord.core.ExternalPointer;
import com.offbynull.peernetic.common.Id;
import java.net.InetSocketAddress;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class FingerTableTest {
    
    public FingerTableTest() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test(expected=IllegalArgumentException.class)
    public void testPut1() { 
        InternalPointer basePtr = TestUtils.generateInternalPointer(2, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        ExternalPointer externalBasePtr = TestUtils.generateExternalPointer(2, 0x00L);
        
        ft.put(externalBasePtr);
    }
    
    @Test
    public void testPut2() { 
        InternalPointer basePtr = TestUtils.generateInternalPointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        ExternalPointer<InetSocketAddress> ptr4 = TestUtils.generateExternalPointer(3, 0x04L);
        
        ft.put(ptr4);
        
        Pointer entry;
        
        entry = ft.get(0);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr4, entry);
        entry = ft.get(2);
        assertEquals(ptr4, entry);
    }

    @Test
    public void testPut3() { 
        InternalPointer basePtr = TestUtils.generateInternalPointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        ExternalPointer<InetSocketAddress> ptr4 = TestUtils.generateExternalPointer(3, 0x04L);
        ExternalPointer<InetSocketAddress> ptr5 = TestUtils.generateExternalPointer(3, 0x05L);
        
        ft.put(ptr4);
        ft.put(ptr5);
        
        Pointer entry;
        
        entry = ft.get(0);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr4, entry);
        entry = ft.get(2);
        assertEquals(ptr5, entry);
    }
    
    @Test
    public void testReplace1() { 
        InternalPointer basePtr = TestUtils.generateInternalPointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        ExternalPointer<InetSocketAddress> ptr4 = TestUtils.generateExternalPointer(3, 0x04L);
        
        ft.replace(ptr4);
        
        Pointer entry;
        
        entry = ft.get(0);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr4, entry);
        entry = ft.get(2);
        assertEquals(ptr4, entry);
    }
    
    @Test
    public void testReplace2() { 
        InternalPointer basePtr = TestUtils.generateInternalPointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        ExternalPointer<InetSocketAddress> ptr4 = TestUtils.generateExternalPointer(3, 0x04L);
        ExternalPointer<InetSocketAddress> ptr5 = TestUtils.generateExternalPointer(3, 0x05L);
        
        ft.put(ptr5);
        ft.replace(ptr4);
        
        Pointer entry;
        
        entry = ft.get(0);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr4, entry);
        entry = ft.get(2);
        assertEquals(ptr4, entry);
    }

    @Test
    public void testReplace3() { 
        InternalPointer basePtr = TestUtils.generateInternalPointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        ExternalPointer<InetSocketAddress> ptr1 = TestUtils.generateExternalPointer(3, 0x01L);
        ExternalPointer<InetSocketAddress> ptr4 = TestUtils.generateExternalPointer(3, 0x04L);
        ExternalPointer<InetSocketAddress> ptr5 = TestUtils.generateExternalPointer(3, 0x05L);
        
        ft.replace(ptr1);
        ft.replace(ptr5);
        ft.replace(ptr4);
        
        Pointer entry;
        
        entry = ft.get(0);
        assertEquals(ptr1, entry);
        entry = ft.get(1);
        assertEquals(ptr4, entry);
        entry = ft.get(2);
        assertEquals(ptr4, entry);
    }
    
    @Test
    public void testEmptyFingerTable() {
        InternalPointer basePtr = TestUtils.generateInternalPointer(2, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        Id id1 = TestUtils.generateId(2, 0x00L);
        Id id2 = TestUtils.generateId(2, 0x01L);
        Id id3 = TestUtils.generateId(2, 0x02L);
        Id id4 = TestUtils.generateId(2, 0x03L);
        
        
        Pointer closestPred;
        
        closestPred = ft.findClosestPreceding(id1);
        assertEquals(basePtr.getId(), closestPred.getId());
        closestPred = ft.findClosestPreceding(id2);
        assertEquals(basePtr.getId(), closestPred.getId());
        closestPred = ft.findClosestPreceding(id3);
        assertEquals(basePtr.getId(), closestPred.getId());
        closestPred = ft.findClosestPreceding(id4);
        assertEquals(basePtr.getId(), closestPred.getId());
    }

    @Test
    public void testPartialFingerTable() {
        InternalPointer basePtr = TestUtils.generateInternalPointer(2, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        
        ExternalPointer<InetSocketAddress> firstNeighbour = TestUtils.generateExternalPointer(2, 0x01L);
        ExternalPointer<InetSocketAddress> secondNeighbour = TestUtils.generateExternalPointer(2, 0x02L);
        ft.put(secondNeighbour);
        ExternalPointer<InetSocketAddress> thirdNeighbour = TestUtils.generateExternalPointer(2, 0x03L);
        
        Pointer closestPred;
        
        closestPred = ft.findClosestPreceding(basePtr.getId());
        assertEquals(basePtr.getId(), closestPred.getId());
        closestPred = ft.findClosestPreceding(firstNeighbour.getId());
        assertEquals(basePtr.getId(), closestPred.getId());
        closestPred = ft.findClosestPreceding(secondNeighbour.getId());
        assertEquals(basePtr.getId(), closestPred.getId());
        closestPred = ft.findClosestPreceding(thirdNeighbour.getId());
        assertEquals(secondNeighbour.getId(), closestPred.getId());
    }
    
    @Test
    public void testFullFingerTable() {
        InternalPointer basePtr = TestUtils.generateInternalPointer(2, 0x00L);
        FingerTable ft = new FingerTable(basePtr);

        ExternalPointer<InetSocketAddress> firstNeighbour = TestUtils.generateExternalPointer(2, 0x01L);
        ft.put(firstNeighbour);
        ExternalPointer<InetSocketAddress> secondNeighbour = TestUtils.generateExternalPointer(2, 0x02L);
        ft.put(secondNeighbour);
        ExternalPointer<InetSocketAddress> thirdNeighbour = TestUtils.generateExternalPointer(2, 0x03L);
        ft.put(thirdNeighbour);
        
        Pointer closestPred;
        
        closestPred = ft.findClosestPreceding(basePtr.getId());
        assertEquals(basePtr.getId(), closestPred.getId());
        closestPred = ft.findClosestPreceding(firstNeighbour.getId());
        assertEquals(basePtr.getId(), closestPred.getId());
        closestPred = ft.findClosestPreceding(secondNeighbour.getId());
        assertEquals(firstNeighbour.getId(), closestPred.getId());
        closestPred = ft.findClosestPreceding(thirdNeighbour.getId());
        assertEquals(firstNeighbour.getId(), closestPred.getId());
    }
    
    @Test
    public void testRemove1() { 
        InternalPointer basePtr = TestUtils.generateInternalPointer(2, 0x02L);
        FingerTable ft = new FingerTable(basePtr);
        ExternalPointer<InetSocketAddress> ptr2 = TestUtils.generateExternalPointer(2, 0x00L);
        ExternalPointer<InetSocketAddress> ptr3 = TestUtils.generateExternalPointer(2, 0x01L);

        ft.put(ptr2);
        ft.remove(ptr3);

        // nothing should be removed
        Pointer entry;
        
        entry = ft.get(1);
        assertEquals(ptr2, entry);
        entry = ft.get(0);
        assertEquals(ptr2, entry);
    }
    
    @Test
    public void testRemove2() { 
        InternalPointer basePtr = TestUtils.generateInternalPointer(2, 0x02L);
        FingerTable ft = new FingerTable(basePtr);
        ExternalPointer<InetSocketAddress> ptr2 = TestUtils.generateExternalPointer(2, 0x00L);
        
        ft.put(ptr2);
        ft.remove(ptr2);

        // table should be reverted
        Pointer entry;
        
        entry = ft.get(0);
        assertEquals(basePtr, entry);
        entry = ft.get(1);
        assertEquals(basePtr, entry);
    }

    @Test
    public void testRemove3() {
        InternalPointer basePtr = TestUtils.generateInternalPointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        ExternalPointer<InetSocketAddress> ptr2 = TestUtils.generateExternalPointer(3, 0x02L);
        ExternalPointer<InetSocketAddress> ptr4 = TestUtils.generateExternalPointer(3, 0x04L);

        ft.put(ptr2);
        ft.put(ptr4);
        ft.remove(ptr4);
        
        // preceding table entry should NOT be replaced
        Pointer entry;
        
        entry = ft.get(2);
        assertEquals(basePtr, entry);
        entry = ft.get(1);
        assertEquals(ptr2, entry);
        entry = ft.get(0);
        assertEquals(ptr2, entry);
    }

    @Test
    public void testClearBefore1() { 
        InternalPointer basePtr = TestUtils.generateInternalPointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        ExternalPointer<InetSocketAddress> ptr2 = TestUtils.generateExternalPointer(3, 0x02L);
        ExternalPointer<InetSocketAddress> ptr4 = TestUtils.generateExternalPointer(3, 0x04L);
        
        ft.put(ptr2);
        ft.put(ptr4);
        int cleared = ft.clearBefore(ptr4.getId());

        assertEquals(1, cleared);
        // should be a clear finger table
        Pointer entry;
        
        entry = ft.get(2);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr4, entry);
        entry = ft.get(0);
        assertEquals(ptr4, entry);
    }

    @Test
    public void testClearBefore2() { 
        InternalPointer basePtr = TestUtils.generateInternalPointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        ExternalPointer<InetSocketAddress> ptr2 = TestUtils.generateExternalPointer(3, 0x02L);
        ExternalPointer<InetSocketAddress> ptr3 = TestUtils.generateExternalPointer(3, 0x03L);
        ExternalPointer<InetSocketAddress> ptr4 = TestUtils.generateExternalPointer(3, 0x04L);
        
        ft.put(ptr2);
        ft.put(ptr4);
        int cleared = ft.clearBefore(ptr3.getId());

        assertEquals(1, cleared);
        
        // should be a clear finger table
        Pointer entry;
        
        entry = ft.get(2);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr4, entry);
        entry = ft.get(0);
        assertEquals(ptr4, entry);
    }

    @Test
    public void testClearBefore3() { 
        InternalPointer basePtr = TestUtils.generateInternalPointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        ExternalPointer<InetSocketAddress> ptr1 = TestUtils.generateExternalPointer(3, 0x01L);
        ExternalPointer<InetSocketAddress> ptr2 = TestUtils.generateExternalPointer(3, 0x02L);
        ExternalPointer<InetSocketAddress> ptr4 = TestUtils.generateExternalPointer(3, 0x04L);
        
        ft.put(ptr2);
        ft.put(ptr4);
        int cleared = ft.clearBefore(ptr1.getId());
        
        assertEquals(0, cleared);

        // should be a clear finger table
        Pointer entry;
        
        entry = ft.get(2);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr2, entry);
        entry = ft.get(0);
        assertEquals(ptr2, entry);
    }

    @Test
    public void testClearAfter1() { 
        InternalPointer basePtr = TestUtils.generateInternalPointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        ExternalPointer<InetSocketAddress> ptr2 = TestUtils.generateExternalPointer(3, 0x02L);
        ExternalPointer<InetSocketAddress> ptr4 = TestUtils.generateExternalPointer(3, 0x04L);
        
        ft.put(ptr2);
        ft.put(ptr4);
        int cleared = ft.clearAfter(basePtr.getId());
        
        assertEquals(3, cleared);

        // should be a clear finger table
        Pointer entry;
        
        entry = ft.get(2);
        assertEquals(basePtr, entry);
        entry = ft.get(1);
        assertEquals(basePtr, entry);
        entry = ft.get(0);
        assertEquals(basePtr, entry);
    }

    @Test
    public void testClearAfter2() { 
        InternalPointer basePtr = TestUtils.generateInternalPointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        ExternalPointer<InetSocketAddress> ptr2 = TestUtils.generateExternalPointer(3, 0x02L);
        ExternalPointer<InetSocketAddress> ptr3 = TestUtils.generateExternalPointer(3, 0x03L);
        ExternalPointer<InetSocketAddress> ptr4 = TestUtils.generateExternalPointer(3, 0x04L);
        
        ft.put(ptr2);
        ft.put(ptr4);
        int cleared = ft.clearAfter(ptr3.getId());
        
        assertEquals(1, cleared);

        // should be a clear finger table
        Pointer entry;
        
        entry = ft.get(2);
        assertEquals(basePtr, entry);
        entry = ft.get(1);
        assertEquals(ptr2, entry);
        entry = ft.get(0);
        assertEquals(ptr2, entry);
    }

    @Test
    public void testClearAfter3() { 
        InternalPointer basePtr = TestUtils.generateInternalPointer(3, 0x00L);
        FingerTable ft = new FingerTable(basePtr);
        ExternalPointer<InetSocketAddress> ptr2 = TestUtils.generateExternalPointer(3, 0x02L);
        ExternalPointer<InetSocketAddress> ptr4 = TestUtils.generateExternalPointer(3, 0x04L);
        ExternalPointer<InetSocketAddress> ptr5 = TestUtils.generateExternalPointer(3, 0x05L);
        
        ft.put(ptr2);
        ft.put(ptr4);
        int cleared = ft.clearAfter(ptr5.getId());
        
        assertEquals(0, cleared);

        // should be a clear finger table
        Pointer entry;
        
        entry = ft.get(2);
        assertEquals(ptr4, entry);
        entry = ft.get(1);
        assertEquals(ptr2, entry);
        entry = ft.get(0);
        assertEquals(ptr2, entry);
    }
}
