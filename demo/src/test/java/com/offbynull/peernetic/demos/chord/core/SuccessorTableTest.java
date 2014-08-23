package com.offbynull.peernetic.demos.chord.core;

import com.offbynull.peernetic.demos.chord.core.SuccessorTable;
import com.offbynull.peernetic.demos.chord.core.Pointer;
import com.offbynull.peernetic.demos.chord.core.InternalPointer;
import com.offbynull.peernetic.demos.chord.core.ExternalPointer;
import java.net.InetSocketAddress;
import java.util.Arrays;
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
        InternalPointer basePtr = TestUtils.generateInternalPointer(2, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        Pointer successorPtr = st.getSuccessor();
        
        assertEquals(basePtr, successorPtr);
    }

    public void testInitialEmpty1() {
        InternalPointer basePtr = TestUtils.generateInternalPointer(2, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        st.moveToNextSucessor();
        assertNull(st.getSuccessor());
    }

    @Test
    public void testUpdate1() {
        InternalPointer basePtr = TestUtils.generateInternalPointer(3, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        ExternalPointer<InetSocketAddress> ptr1 = TestUtils.generateExternalPointer(3, 0x01L);
        ExternalPointer<InetSocketAddress> ptr2 = TestUtils.generateExternalPointer(3, 0x02L);
        ExternalPointer<InetSocketAddress> ptr3 = TestUtils.generateExternalPointer(3, 0x03L);
        ExternalPointer<InetSocketAddress> ptr4 = TestUtils.generateExternalPointer(3, 0x04L);
        ExternalPointer<InetSocketAddress> ptr5 = TestUtils.generateExternalPointer(3, 0x05L);
        ExternalPointer<InetSocketAddress> ptr6 = TestUtils.generateExternalPointer(3, 0x06L);
        ExternalPointer<InetSocketAddress> ptr7 = TestUtils.generateExternalPointer(3, 0x07L);
        
        st.update(ptr1,
                Arrays.asList(ptr2, ptr3, ptr4, ptr5, ptr6, ptr7,
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
        InternalPointer basePtr = TestUtils.generateInternalPointer(3, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        ExternalPointer<InetSocketAddress> ptr1 = TestUtils.generateExternalPointer(3, 0x01L);
        ExternalPointer<InetSocketAddress> ptr2 = TestUtils.generateExternalPointer(3, 0x02L);
        
        st.update(ptr1,
                Arrays.asList(ptr2, basePtr, ptr1, ptr2, basePtr, ptr1,
                ptr2));
        
        assertEquals(ptr1, st.getSuccessor());
        st.moveToNextSucessor();
        assertEquals(ptr2, st.getSuccessor());
    }

    @Test(expected = IllegalStateException.class)
    public void testUpdateFail1() {
        InternalPointer basePtr = TestUtils.generateInternalPointer(3, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        ExternalPointer<InetSocketAddress> ptr1 = TestUtils.generateExternalPointer(3, 0x01L);
        ExternalPointer<InetSocketAddress> ptr2 = TestUtils.generateExternalPointer(3, 0x02L);
        
        st.update(ptr1,
                Arrays.asList(ptr2, basePtr, ptr1, ptr2, basePtr, ptr1,
                ptr2));
        
        assertEquals(ptr1, st.getSuccessor());
        st.moveToNextSucessor();
        assertEquals(ptr2, st.getSuccessor());
        st.moveToNextSucessor();
    }
    
    @Test
    public void testUpdateTrim1() {
        InternalPointer basePtr = TestUtils.generateInternalPointer(3, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        ExternalPointer<InetSocketAddress> ptr1 = TestUtils.generateExternalPointer(3, 0x01L);
        ExternalPointer<InetSocketAddress> ptr2 = TestUtils.generateExternalPointer(3, 0x02L);
        ExternalPointer<InetSocketAddress> ptr3 = TestUtils.generateExternalPointer(3, 0x03L);
        ExternalPointer<InetSocketAddress> ptr4 = TestUtils.generateExternalPointer(3, 0x04L);
        ExternalPointer<InetSocketAddress> ptr5 = TestUtils.generateExternalPointer(3, 0x05L);
        ExternalPointer<InetSocketAddress> ptr6 = TestUtils.generateExternalPointer(3, 0x06L);
        ExternalPointer<InetSocketAddress> ptr7 = TestUtils.generateExternalPointer(3, 0x07L);
        
        st.update(ptr1,
                Arrays.asList(ptr2, ptr4, ptr5, ptr6, ptr7,
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
        InternalPointer basePtr = TestUtils.generateInternalPointer(3, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        ExternalPointer<InetSocketAddress> ptr1 = TestUtils.generateExternalPointer(3, 0x01L);
        ExternalPointer<InetSocketAddress> ptr2 = TestUtils.generateExternalPointer(3, 0x02L);
        ExternalPointer<InetSocketAddress> ptr3 = TestUtils.generateExternalPointer(3, 0x03L);
        ExternalPointer<InetSocketAddress> ptr4 = TestUtils.generateExternalPointer(3, 0x04L);
        ExternalPointer<InetSocketAddress> ptr5 = TestUtils.generateExternalPointer(3, 0x05L);
        ExternalPointer<InetSocketAddress> ptr6 = TestUtils.generateExternalPointer(3, 0x06L);
        ExternalPointer<InetSocketAddress> ptr7 = TestUtils.generateExternalPointer(3, 0x07L);
        
        st.update(ptr1,
                Arrays.asList(ptr2, ptr3, ptr4, ptr5, ptr6, ptr7,
                basePtr));
        st.updateTrim(ptr3);
        
        assertEquals(ptr3, st.getSuccessor());
        st.moveToNextSucessor();
        assertEquals(ptr4, st.getSuccessor());
    }

    @Test
    public void testUpdateTrim3() {
        InternalPointer basePtr = TestUtils.generateInternalPointer(3, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        ExternalPointer<InetSocketAddress> ptr1 = TestUtils.generateExternalPointer(3, 0x01L);
        ExternalPointer<InetSocketAddress> ptr2 = TestUtils.generateExternalPointer(3, 0x02L);
        ExternalPointer<InetSocketAddress> ptr3 = TestUtils.generateExternalPointer(3, 0x03L);
        ExternalPointer<InetSocketAddress> ptr4 = TestUtils.generateExternalPointer(3, 0x04L);
        ExternalPointer<InetSocketAddress> ptr5 = TestUtils.generateExternalPointer(3, 0x05L);
        ExternalPointer<InetSocketAddress> ptr6 = TestUtils.generateExternalPointer(3, 0x06L);
        ExternalPointer<InetSocketAddress> ptr7 = TestUtils.generateExternalPointer(3, 0x07L);
        
        st.update(ptr1,
                Arrays.asList(ptr2, ptr3, ptr4, ptr5, ptr6, ptr7,
                basePtr));
        st.updateTrim(ptr7);
        
        assertEquals(ptr7, st.getSuccessor());
    }
}