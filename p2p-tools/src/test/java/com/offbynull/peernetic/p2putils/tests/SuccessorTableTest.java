package com.offbynull.peernetic.p2putils.tests;

import com.google.common.collect.Lists;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedPointer;
import com.offbynull.peernetic.p2ptools.overlay.structured.chord.SuccessorTable;
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
        BitLimitedPointer basePtr = TestUtils.generatePointer(2, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        BitLimitedPointer successorPtr = st.getSuccessor();
        
        assertEquals(basePtr, successorPtr);
    }

    public void testInitialEmpty1() {
        BitLimitedPointer basePtr = TestUtils.generatePointer(2, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        st.moveToNextSucessor();
        assertNull(st.getSuccessor());
    }

    @Test
    public void testUpdate1() {
        BitLimitedPointer basePtr = TestUtils.generatePointer(3, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        BitLimitedPointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        BitLimitedPointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        BitLimitedPointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        BitLimitedPointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        BitLimitedPointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        BitLimitedPointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        BitLimitedPointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
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
        BitLimitedPointer basePtr = TestUtils.generatePointer(3, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        BitLimitedPointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        BitLimitedPointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        
        st.update(ptr1,
                Lists.newArrayList(ptr2, basePtr, ptr1, ptr2, basePtr, ptr1,
                ptr2));
        
        assertEquals(ptr1, st.getSuccessor());
        st.moveToNextSucessor();
        assertEquals(ptr2, st.getSuccessor());
    }

    @Test(expected = IllegalStateException.class)
    public void testUpdateFail1() {
        BitLimitedPointer basePtr = TestUtils.generatePointer(3, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        BitLimitedPointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        BitLimitedPointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        
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
        BitLimitedPointer basePtr = TestUtils.generatePointer(3, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        BitLimitedPointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        BitLimitedPointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        BitLimitedPointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        BitLimitedPointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        BitLimitedPointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        BitLimitedPointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        BitLimitedPointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
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
        BitLimitedPointer basePtr = TestUtils.generatePointer(3, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        BitLimitedPointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        BitLimitedPointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        BitLimitedPointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        BitLimitedPointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        BitLimitedPointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        BitLimitedPointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        BitLimitedPointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
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
        BitLimitedPointer basePtr = TestUtils.generatePointer(3, 0L);
        SuccessorTable st = new SuccessorTable(basePtr);
        
        BitLimitedPointer ptr1 = TestUtils.generatePointer(3, 0x01L);
        BitLimitedPointer ptr2 = TestUtils.generatePointer(3, 0x02L);
        BitLimitedPointer ptr3 = TestUtils.generatePointer(3, 0x03L);
        BitLimitedPointer ptr4 = TestUtils.generatePointer(3, 0x04L);
        BitLimitedPointer ptr5 = TestUtils.generatePointer(3, 0x05L);
        BitLimitedPointer ptr6 = TestUtils.generatePointer(3, 0x06L);
        BitLimitedPointer ptr7 = TestUtils.generatePointer(3, 0x07L);
        
        st.update(ptr1,
                Lists.newArrayList(ptr2, ptr3, ptr4, ptr5, ptr6, ptr7,
                basePtr));
        st.updateTrim(ptr7);
        
        assertEquals(ptr7, st.getSuccessor());
    }
}