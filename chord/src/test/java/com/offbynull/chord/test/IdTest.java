package com.offbynull.chord.test;

import com.offbynull.chord.Id;
import java.math.BigInteger;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class IdTest {

    public IdTest() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testCreateNormal1() {
        Id id = new Id(2, new byte[]{0x01});
        assertEquals(BigInteger.ONE, id.asBigInteger());
    }

    @Test
    public void testCreateNormal2() {
        Id id = new Id(8, new byte[]{0x11});
        assertEquals(new BigInteger("17"), id.asBigInteger());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNormal3() {
        Id id = new Id(0, new byte[]{0x11});
        assertEquals(new BigInteger("0"), id.asBigInteger());
    }

    @Test
    public void testCreateOverflow1() {
        Id id = new Id(2, new byte[]{0x76, 0x51});
        assertEquals(BigInteger.ONE, id.asBigInteger());
    }

    @Test
    public void testCreateOverflow2() {
        Id id = new Id(12, new byte[]{0x76, 0x51});
        assertEquals(new BigInteger("1617"), id.asBigInteger());
    }

    @Test
    public void testCreateUnderflow() {
        Id id = new Id(256, new byte[]{0x76, 0x51});
        assertEquals(new BigInteger("30289"), id.asBigInteger());
    }

    @Test
    public void testAdd1() {
        Id id1 = new Id(2, new byte[]{0x01});
        Id id2 = new Id(2, new byte[]{0x02});
        Id addedId = id1.add(id2);
        assertEquals(new BigInteger("3"), addedId.asBigInteger());
    }

    @Test
    public void testAdd2() {
        Id id1 = new Id(2, new byte[]{0x01});
        Id id2 = new Id(2, new byte[]{0x03});
        Id addedId = id1.add(id2);
        assertEquals(new BigInteger("0"), addedId.asBigInteger());
    }

    @Test
    public void testAdd3() {
        Id id1 = new Id(2, new byte[]{0x51});
        Id id2 = new Id(2, new byte[]{0x63});
        Id addedId = id1.add(id2);
        assertEquals(new BigInteger("0"), addedId.asBigInteger());
    }

    @Test
    public void testAdd4() {
        Id id1 = new Id(2, new byte[]{(byte) 0xFF});
        Id id2 = new Id(2, new byte[]{(byte) 0xFF});
        Id addedId = id1.add(id2);
        assertEquals(new BigInteger("2"), addedId.asBigInteger());
    }

    @Test
    public void testSubtract1() {
        Id id1 = new Id(2, new byte[]{0x03});
        Id id2 = new Id(2, new byte[]{0x01});
        Id addedId = id1.subtract(id2);
        assertEquals(new BigInteger("2"), addedId.asBigInteger());
    }

    @Test
    public void testSubtract2() {
        Id id1 = new Id(2, new byte[]{0x01});
        Id id2 = new Id(2, new byte[]{0x02});
        Id addedId = id1.subtract(id2);
        assertEquals(new BigInteger("3"), addedId.asBigInteger());
    }

    @Test
    public void testSubtract3() {
        Id id1 = new Id(2, new byte[]{0x51});
        Id id2 = new Id(2, new byte[]{0x63});
        Id addedId = id1.subtract(id2);
        assertEquals(new BigInteger("2"), addedId.asBigInteger());
    }

    @Test
    public void testSubtract4() {
        Id id1 = new Id(2, new byte[]{(byte) 0xFF});
        Id id2 = new Id(2, new byte[]{(byte) 0xFF});
        Id addedId = id1.subtract(id2);
        assertEquals(new BigInteger("0"), addedId.asBigInteger());
    }

    @Test
    public void testSubtract5() {
        Id id1 = new Id(9, new byte[]{0x11, (byte) 0xFF});
        Id id2 = new Id(9, new byte[]{0x60, (byte) 0xFF});
        Id addedId = id1.subtract(id2);
        assertEquals(new BigInteger("256"), addedId.asBigInteger());
    }

    @Test
    public void testComparePosition1() {
        Id baseId = new Id(2, new byte[]{0x01});
        Id id1 = new Id(2, new byte[]{0x00});
        Id id2 = new Id(2, new byte[]{0x02});
        int compResult = id1.comparePosition(baseId, id2);
        assertEquals(1, compResult);
    }

    @Test
    public void testComparePosition2() {
        Id baseId = new Id(2, new byte[]{0x01});
        Id id1 = new Id(2, new byte[]{0x02});
        Id id2 = new Id(2, new byte[]{0x00});
        int compResult = id1.comparePosition(baseId, id2);
        assertEquals(-1, compResult);
    }

    @Test
    public void testComparePosition3() {
        Id baseId = new Id(2, new byte[]{0x01});
        Id id1 = new Id(2, new byte[]{0x02});
        Id id2 = new Id(2, new byte[]{0x02});
        int compResult = id1.comparePosition(baseId, id2);
        assertEquals(0, compResult);
    }
    
    @Test
    public void testIsWithin1() {
        Id baseId = new Id(2, new byte[]{0x00});
        Id testId = new Id(2, new byte[]{0x02});
        Id startId = new Id(2, new byte[]{0x01});
        Id endId = new Id(2, new byte[]{0x03});
        
        boolean res;
        
        res = testId.isWithin(baseId, startId, true, endId, true);
        assertEquals(res, true);
        res = testId.isWithin(baseId, startId, true, endId, false);
        assertEquals(res, true);
        res = testId.isWithin(baseId, startId, false, endId, true);
        assertEquals(res, true);
        res = testId.isWithin(baseId, startId, false, endId, false);
        assertEquals(res, true);
    }

    @Test
    public void testIsWithin2() {
        Id baseId = new Id(2, new byte[]{0x00});
        Id testId = new Id(2, new byte[]{0x01});
        Id startId = new Id(2, new byte[]{0x01});
        Id endId = new Id(2, new byte[]{0x03});
        
        boolean res;
        
        res = testId.isWithin(baseId, startId, true, endId, true);
        assertEquals(res, true);
        res = testId.isWithin(baseId, startId, true, endId, false);
        assertEquals(res, true);
        res = testId.isWithin(baseId, startId, false, endId, true);
        assertEquals(res, false);
        res = testId.isWithin(baseId, startId, false, endId, false);
        assertEquals(res, false);
    }

    @Test
    public void testIsWithin3() {
        Id baseId = new Id(2, new byte[]{0x00});
        Id testId = new Id(2, new byte[]{0x03});
        Id startId = new Id(2, new byte[]{0x01});
        Id endId = new Id(2, new byte[]{0x03});
        
        boolean res;
        
        res = testId.isWithin(baseId, startId, true, endId, true);
        assertEquals(res, true);
        res = testId.isWithin(baseId, startId, true, endId, false);
        assertEquals(res, false);
        res = testId.isWithin(baseId, startId, false, endId, true);
        assertEquals(res, true);
        res = testId.isWithin(baseId, startId, false, endId, false);
        assertEquals(res, false);
    }

    @Test
    public void testIsWithin4() {
        Id baseId = new Id(2, new byte[]{0x00});
        Id testId = new Id(2, new byte[]{0x00});
        Id startId = new Id(2, new byte[]{0x01});
        Id endId = new Id(2, new byte[]{0x03});
        
        boolean res;
        
        res = testId.isWithin(baseId, startId, true, endId, true);
        assertEquals(res, false);
        res = testId.isWithin(baseId, startId, true, endId, false);
        assertEquals(res, false);
        res = testId.isWithin(baseId, startId, false, endId, true);
        assertEquals(res, false);
        res = testId.isWithin(baseId, startId, false, endId, false);
        assertEquals(res, false);
    }

    @Test
    public void testIsWithin5() {
        Id baseId = new Id(2, new byte[]{0x01});
        Id testId = new Id(2, new byte[]{0x03});
        Id startId = new Id(2, new byte[]{0x02});
        Id endId = new Id(2, new byte[]{0x00});
        
        boolean res;
        
        res = testId.isWithin(baseId, startId, true, endId, true);
        assertEquals(res, true);
        res = testId.isWithin(baseId, startId, true, endId, false);
        assertEquals(res, true);
        res = testId.isWithin(baseId, startId, false, endId, true);
        assertEquals(res, true);
        res = testId.isWithin(baseId, startId, false, endId, false);
        assertEquals(res, true);
    }

    @Test
    public void testIsWithin6() {
        Id baseId = new Id(2, new byte[]{0x01});
        Id testId = new Id(2, new byte[]{0x02});
        Id startId = new Id(2, new byte[]{0x02});
        Id endId = new Id(2, new byte[]{0x00});
        
        boolean res;
        
        res = testId.isWithin(baseId, startId, true, endId, true);
        assertEquals(res, true);
        res = testId.isWithin(baseId, startId, true, endId, false);
        assertEquals(res, true);
        res = testId.isWithin(baseId, startId, false, endId, true);
        assertEquals(res, false);
        res = testId.isWithin(baseId, startId, false, endId, false);
        assertEquals(res, false);
    }

    @Test
    public void testIsWithin7() {
        Id baseId = new Id(2, new byte[]{0x01});
        Id testId = new Id(2, new byte[]{0x00});
        Id startId = new Id(2, new byte[]{0x02});
        Id endId = new Id(2, new byte[]{0x00});
        
        boolean res;
        
        res = testId.isWithin(baseId, startId, true, endId, true);
        assertEquals(res, true);
        res = testId.isWithin(baseId, startId, true, endId, false);
        assertEquals(res, false);
        res = testId.isWithin(baseId, startId, false, endId, true);
        assertEquals(res, true);
        res = testId.isWithin(baseId, startId, false, endId, false);
        assertEquals(res, false);
    }

    @Test
    public void testIsWithin8() {
        Id baseId = new Id(2, new byte[]{0x01});
        Id testId = new Id(2, new byte[]{0x01});
        Id startId = new Id(2, new byte[]{0x02});
        Id endId = new Id(2, new byte[]{0x00});
        
        boolean res;
        
        res = testId.isWithin(baseId, startId, true, endId, true);
        assertEquals(res, false);
        res = testId.isWithin(baseId, startId, true, endId, false);
        assertEquals(res, false);
        res = testId.isWithin(baseId, startId, false, endId, true);
        assertEquals(res, false);
        res = testId.isWithin(baseId, startId, false, endId, false);
        assertEquals(res, false);
    }
}
