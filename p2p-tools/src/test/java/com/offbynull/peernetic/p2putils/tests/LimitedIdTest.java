package com.offbynull.peernetic.p2putils.tests;

import com.offbynull.peernetic.p2ptools.identification.LimitedId;
import java.math.BigInteger;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class LimitedIdTest {

    public LimitedIdTest() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testCreateNormal1() {
        LimitedId id = new LimitedId(new byte[]{0x01}, new byte[]{0x03});
        assertEquals(BigInteger.ONE, id.asBigInteger());
    }

    @Test
    public void testCreateNormal2() {
        LimitedId id = new LimitedId(new byte[]{0x11}, new byte[]{(byte) 0xFF});
        assertEquals(new BigInteger("17"), id.asBigInteger());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNormal3() {
        LimitedId id = new LimitedId(new byte[]{0x11}, new byte[0]);
        assertEquals(new BigInteger("0"), id.asBigInteger());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateOverflow1() {
        LimitedId id = new LimitedId(new byte[]{0x76, 0x51}, new byte[] {0x03});
        assertEquals(BigInteger.ONE, id.asBigInteger());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateOverflow2() {
        LimitedId id = new LimitedId(new byte[]{0x76, 0x51},
                new byte[] {(byte) 0x0F, (byte) 0xFF});
        assertEquals(new BigInteger("1617"), id.asBigInteger());
    }

    @Test
    public void testCreateUnderflow() {
        LimitedId id = new LimitedId(new byte[]{0x76, 0x51},
                new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
        assertEquals(new BigInteger("30289"), id.asBigInteger());
    }

    @Test
    public void testAdd1() {
        LimitedId id1 = new LimitedId(new byte[]{0x01}, new byte[]{0x03});
        LimitedId id2 = new LimitedId(new byte[]{0x02}, new byte[]{0x03});
        LimitedId addedId = id1.add(id2);
        assertEquals(new BigInteger("3"), addedId.asBigInteger());
    }

    @Test
    public void testAdd2() {
        LimitedId id1 = new LimitedId(new byte[]{0x01}, new byte[]{0x03});
        LimitedId id2 = new LimitedId(new byte[]{0x03}, new byte[]{0x03});
        LimitedId addedId = id1.add(id2);
        assertEquals(new BigInteger("0"), addedId.asBigInteger());
    }

    @Test
    public void testAdd3() {
        LimitedId id1 = new LimitedId(new byte[]{0x02}, new byte[]{0x03});
        LimitedId id2 = new LimitedId(new byte[]{0x03}, new byte[]{0x03});
        LimitedId addedId = id1.add(id2);
        assertEquals(new BigInteger("1"), addedId.asBigInteger());
    }

    @Test
    public void testAdd4() {
        LimitedId id1 = new LimitedId(new byte[]{(byte) 0x03}, new byte[]{0x03});
        LimitedId id2 = new LimitedId(new byte[]{(byte) 0x03}, new byte[]{0x03});
        LimitedId addedId = id1.add(id2);
        assertEquals(new BigInteger("2"), addedId.asBigInteger());
    }

    @Test
    public void testSubtract1() {
        LimitedId id1 = new LimitedId(new byte[]{0x03}, new byte[]{0x03});
        LimitedId id2 = new LimitedId(new byte[]{0x01}, new byte[]{0x03});
        LimitedId addedId = id1.subtract(id2);
        assertEquals(new BigInteger("2"), addedId.asBigInteger());
    }

    @Test
    public void testSubtract2() {
        LimitedId id1 = new LimitedId(new byte[]{0x01}, new byte[]{0x03});
        LimitedId id2 = new LimitedId(new byte[]{0x02}, new byte[]{0x03});
        LimitedId addedId = id1.subtract(id2);
        assertEquals(new BigInteger("3"), addedId.asBigInteger());
    }

    @Test
    public void testSubtract3() {
        LimitedId id1 = new LimitedId(new byte[]{0x01}, new byte[]{0x03});
        LimitedId id2 = new LimitedId(new byte[]{0x03}, new byte[]{0x03});
        LimitedId addedId = id1.subtract(id2);
        assertEquals(new BigInteger("2"), addedId.asBigInteger());
    }

    @Test
    public void testSubtract4() {
        LimitedId id1 = new LimitedId(new byte[]{(byte) 0x03}, new byte[]{0x03});
        LimitedId id2 = new LimitedId(new byte[]{(byte) 0x03}, new byte[]{0x03});
        LimitedId addedId = id1.subtract(id2);
        assertEquals(new BigInteger("0"), addedId.asBigInteger());
    }

    @Test
    public void testSubtract5() {
        LimitedId id1 = new LimitedId(new byte[]{0x01, (byte) 0xFF},
                new byte[]{0x01, (byte) 0xFF});
        LimitedId id2 = new LimitedId(new byte[]{0x00, (byte) 0xFF},
                new byte[]{0x01, (byte) 0xFF});
        LimitedId addedId = id1.subtract(id2);
        assertEquals(new BigInteger("256"), addedId.asBigInteger());
    }

    @Test
    public void testComparePosition1() {
        LimitedId baseId = new LimitedId(new byte[]{0x01}, new byte[]{0x03});
        LimitedId id1 = new LimitedId(new byte[]{0x00}, new byte[]{0x03});
        LimitedId id2 = new LimitedId(new byte[]{0x02}, new byte[]{0x03});
        int compResult = id1.comparePosition(baseId, id2);
        assertEquals(1, compResult);
    }

    @Test
    public void testComparePosition2() {
        LimitedId baseId = new LimitedId(new byte[]{0x01}, new byte[]{0x03});
        LimitedId id1 = new LimitedId(new byte[]{0x02}, new byte[]{0x03});
        LimitedId id2 = new LimitedId(new byte[]{0x00}, new byte[]{0x03});
        int compResult = id1.comparePosition(baseId, id2);
        assertEquals(-1, compResult);
    }

    @Test
    public void testComparePosition3() {
        LimitedId baseId = new LimitedId(new byte[]{0x01}, new byte[]{0x03});
        LimitedId id1 = new LimitedId(new byte[]{0x02}, new byte[]{0x03});
        LimitedId id2 = new LimitedId(new byte[]{0x02}, new byte[]{0x03});
        int compResult = id1.comparePosition(baseId, id2);
        assertEquals(0, compResult);
    }
    
    @Test
    public void testIsWithin1() {
        LimitedId baseId = new LimitedId(new byte[]{0x00}, new byte[]{0x03});
        LimitedId testId = new LimitedId(new byte[]{0x02}, new byte[]{0x03});
        LimitedId startId = new LimitedId(new byte[]{0x01}, new byte[]{0x03});
        LimitedId endId = new LimitedId(new byte[]{0x03}, new byte[]{0x03});
        
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
        LimitedId baseId = new LimitedId(new byte[]{0x00}, new byte[]{0x03});
        LimitedId testId = new LimitedId(new byte[]{0x01}, new byte[]{0x03});
        LimitedId startId = new LimitedId(new byte[]{0x01}, new byte[]{0x03});
        LimitedId endId = new LimitedId(new byte[]{0x03}, new byte[]{0x03});
        
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
        LimitedId baseId = new LimitedId(new byte[]{0x00}, new byte[]{0x03});
        LimitedId testId = new LimitedId(new byte[]{0x03}, new byte[]{0x03});
        LimitedId startId = new LimitedId(new byte[]{0x01}, new byte[]{0x03});
        LimitedId endId = new LimitedId(new byte[]{0x03}, new byte[]{0x03});
        
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
        LimitedId baseId = new LimitedId(new byte[]{0x00}, new byte[]{0x03});
        LimitedId testId = new LimitedId(new byte[]{0x00}, new byte[]{0x03});
        LimitedId startId = new LimitedId(new byte[]{0x01}, new byte[]{0x03});
        LimitedId endId = new LimitedId(new byte[]{0x03}, new byte[]{0x03});
        
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
        LimitedId baseId = new LimitedId(new byte[]{0x01}, new byte[]{0x03});
        LimitedId testId = new LimitedId(new byte[]{0x03}, new byte[]{0x03});
        LimitedId startId = new LimitedId(new byte[]{0x02}, new byte[]{0x03});
        LimitedId endId = new LimitedId(new byte[]{0x00}, new byte[]{0x03});
        
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
        LimitedId baseId = new LimitedId(new byte[]{0x01}, new byte[]{0x03});
        LimitedId testId = new LimitedId(new byte[]{0x02}, new byte[]{0x03});
        LimitedId startId = new LimitedId(new byte[]{0x02}, new byte[]{0x03});
        LimitedId endId = new LimitedId(new byte[]{0x00}, new byte[]{0x03});
        
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
        LimitedId baseId = new LimitedId(new byte[]{0x01}, new byte[]{0x03});
        LimitedId testId = new LimitedId(new byte[]{0x00}, new byte[]{0x03});
        LimitedId startId = new LimitedId(new byte[]{0x02}, new byte[]{0x03});
        LimitedId endId = new LimitedId(new byte[]{0x00}, new byte[]{0x03});
        
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
        LimitedId baseId = new LimitedId(new byte[]{0x01}, new byte[]{0x03});
        LimitedId testId = new LimitedId(new byte[]{0x01}, new byte[]{0x03});
        LimitedId startId = new LimitedId(new byte[]{0x02}, new byte[]{0x03});
        LimitedId endId = new LimitedId(new byte[]{0x00}, new byte[]{0x03});
        
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
