package com.offbynull.peernetic.overlay.common.id;

import com.offbynull.peernetic.overlay.common.id.BitLimitedId;
import java.math.BigInteger;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class BitLimitedIdTest {

    public BitLimitedIdTest() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testCreateNormal1() {
        BitLimitedId id = new BitLimitedId(2, new byte[]{0x01});
        assertEquals(BigInteger.ONE, id.asBigInteger());
    }

    @Test
    public void testCreateNormal2() {
        BitLimitedId id = new BitLimitedId(8, new byte[]{0x11});
        assertEquals(new BigInteger("17"), id.asBigInteger());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNormal3() {
        BitLimitedId id = new BitLimitedId(0, new byte[]{0x11});
        assertEquals(new BigInteger("0"), id.asBigInteger());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateOverflow1() {
        BitLimitedId id = new BitLimitedId(2, new byte[]{0x76, 0x51});
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCreateOverflow2() {
        BitLimitedId id = new BitLimitedId(12, new byte[]{0x76, 0x51});
    }

    @Test
    public void testCreateUnderflow() {
        BitLimitedId id = new BitLimitedId(256, new byte[]{0x76, 0x51});
        assertEquals(new BigInteger("30289"), id.asBigInteger());
    }

    @Test
    public void testAdd1() {
        BitLimitedId id1 = new BitLimitedId(2, new byte[]{0x01});
        BitLimitedId id2 = new BitLimitedId(2, new byte[]{0x02});
        BitLimitedId addedId = id1.add(id2);
        assertEquals(new BigInteger("3"), addedId.asBigInteger());
    }

    @Test
    public void testAdd2() {
        BitLimitedId id1 = new BitLimitedId(2, new byte[]{0x01});
        BitLimitedId id2 = new BitLimitedId(2, new byte[]{0x03});
        BitLimitedId addedId = id1.add(id2);
        assertEquals(new BigInteger("0"), addedId.asBigInteger());
    }

    @Test
    public void testAdd3() {
        BitLimitedId id1 = new BitLimitedId(2, new byte[]{0x01});
        BitLimitedId id2 = new BitLimitedId(2, new byte[]{0x03});
        BitLimitedId addedId = id1.add(id2);
        assertEquals(new BigInteger("0"), addedId.asBigInteger());
    }

    @Test
    public void testAdd4() {
        BitLimitedId id1 = new BitLimitedId(2, new byte[]{0x03});
        BitLimitedId id2 = new BitLimitedId(2, new byte[]{0x03});
        BitLimitedId addedId = id1.add(id2);
        assertEquals(new BigInteger("2"), addedId.asBigInteger());
    }

    @Test
    public void testSubtract1() {
        BitLimitedId id1 = new BitLimitedId(2, new byte[]{0x03});
        BitLimitedId id2 = new BitLimitedId(2, new byte[]{0x01});
        BitLimitedId addedId = id1.subtract(id2);
        assertEquals(new BigInteger("2"), addedId.asBigInteger());
    }

    @Test
    public void testSubtract2() {
        BitLimitedId id1 = new BitLimitedId(2, new byte[]{0x01});
        BitLimitedId id2 = new BitLimitedId(2, new byte[]{0x02});
        BitLimitedId addedId = id1.subtract(id2);
        assertEquals(new BigInteger("3"), addedId.asBigInteger());
    }

    @Test
    public void testSubtract3() {
        BitLimitedId id1 = new BitLimitedId(2, new byte[]{0x01});
        BitLimitedId id2 = new BitLimitedId(2, new byte[]{0x03});
        BitLimitedId addedId = id1.subtract(id2);
        assertEquals(new BigInteger("2"), addedId.asBigInteger());
    }

    @Test
    public void testSubtract4() {
        BitLimitedId id1 = new BitLimitedId(2, new byte[]{(byte) 0x03});
        BitLimitedId id2 = new BitLimitedId(2, new byte[]{(byte) 0x03});
        BitLimitedId addedId = id1.subtract(id2);
        assertEquals(new BigInteger("0"), addedId.asBigInteger());
    }

    @Test
    public void testSubtract5() {
        BitLimitedId id1 = new BitLimitedId(9, new byte[]{0x01, (byte) 0xFF});
        BitLimitedId id2 = new BitLimitedId(9, new byte[]{0x00, (byte) 0xFF});
        BitLimitedId addedId = id1.subtract(id2);
        assertEquals(new BigInteger("256"), addedId.asBigInteger());
    }

    @Test
    public void testComparePosition1() {
        BitLimitedId baseId = new BitLimitedId(2, new byte[]{0x01});
        BitLimitedId id1 = new BitLimitedId(2, new byte[]{0x00});
        BitLimitedId id2 = new BitLimitedId(2, new byte[]{0x02});
        int compResult = id1.comparePosition(baseId, id2);
        assertEquals(1, compResult);
    }

    @Test
    public void testComparePosition2() {
        BitLimitedId baseId = new BitLimitedId(2, new byte[]{0x01});
        BitLimitedId id1 = new BitLimitedId(2, new byte[]{0x02});
        BitLimitedId id2 = new BitLimitedId(2, new byte[]{0x00});
        int compResult = id1.comparePosition(baseId, id2);
        assertEquals(-1, compResult);
    }

    @Test
    public void testComparePosition3() {
        BitLimitedId baseId = new BitLimitedId(2, new byte[]{0x01});
        BitLimitedId id1 = new BitLimitedId(2, new byte[]{0x02});
        BitLimitedId id2 = new BitLimitedId(2, new byte[]{0x02});
        int compResult = id1.comparePosition(baseId, id2);
        assertEquals(0, compResult);
    }
    
    @Test
    public void testIsWithin1() {
        BitLimitedId baseId = new BitLimitedId(2, new byte[]{0x00});
        BitLimitedId testId = new BitLimitedId(2, new byte[]{0x02});
        BitLimitedId startId = new BitLimitedId(2, new byte[]{0x01});
        BitLimitedId endId = new BitLimitedId(2, new byte[]{0x03});
        
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
        BitLimitedId baseId = new BitLimitedId(2, new byte[]{0x00});
        BitLimitedId testId = new BitLimitedId(2, new byte[]{0x01});
        BitLimitedId startId = new BitLimitedId(2, new byte[]{0x01});
        BitLimitedId endId = new BitLimitedId(2, new byte[]{0x03});
        
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
        BitLimitedId baseId = new BitLimitedId(2, new byte[]{0x00});
        BitLimitedId testId = new BitLimitedId(2, new byte[]{0x03});
        BitLimitedId startId = new BitLimitedId(2, new byte[]{0x01});
        BitLimitedId endId = new BitLimitedId(2, new byte[]{0x03});
        
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
        BitLimitedId baseId = new BitLimitedId(2, new byte[]{0x00});
        BitLimitedId testId = new BitLimitedId(2, new byte[]{0x00});
        BitLimitedId startId = new BitLimitedId(2, new byte[]{0x01});
        BitLimitedId endId = new BitLimitedId(2, new byte[]{0x03});
        
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
        BitLimitedId baseId = new BitLimitedId(2, new byte[]{0x01});
        BitLimitedId testId = new BitLimitedId(2, new byte[]{0x03});
        BitLimitedId startId = new BitLimitedId(2, new byte[]{0x02});
        BitLimitedId endId = new BitLimitedId(2, new byte[]{0x00});
        
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
        BitLimitedId baseId = new BitLimitedId(2, new byte[]{0x01});
        BitLimitedId testId = new BitLimitedId(2, new byte[]{0x02});
        BitLimitedId startId = new BitLimitedId(2, new byte[]{0x02});
        BitLimitedId endId = new BitLimitedId(2, new byte[]{0x00});
        
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
        BitLimitedId baseId = new BitLimitedId(2, new byte[]{0x01});
        BitLimitedId testId = new BitLimitedId(2, new byte[]{0x00});
        BitLimitedId startId = new BitLimitedId(2, new byte[]{0x02});
        BitLimitedId endId = new BitLimitedId(2, new byte[]{0x00});
        
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
        BitLimitedId baseId = new BitLimitedId(2, new byte[]{0x01});
        BitLimitedId testId = new BitLimitedId(2, new byte[]{0x01});
        BitLimitedId startId = new BitLimitedId(2, new byte[]{0x02});
        BitLimitedId endId = new BitLimitedId(2, new byte[]{0x00});
        
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
