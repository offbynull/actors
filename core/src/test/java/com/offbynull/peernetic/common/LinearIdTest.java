package com.offbynull.peernetic.common;

import java.math.BigInteger;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public final class LinearIdTest {

    @Test
    public void testCreateNormal1() {
        LinearId id = new LinearId(new byte[]{0x01}, new byte[]{0x03});
        assertEquals(BigInteger.ONE, id.getValueAsBigInteger());
    }

    @Test
    public void testCreateNormal2() {
        LinearId id = new LinearId(new byte[]{0x11}, new byte[]{(byte) 0xFF});
        assertEquals(new BigInteger("17"), id.getValueAsBigInteger());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNormal3() {
        LinearId id = new LinearId(new byte[]{0x11}, new byte[0]);
        assertEquals(new BigInteger("0"), id.getValueAsBigInteger());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateOverflow1() {
        LinearId id = new LinearId(new byte[]{0x76, 0x51}, new byte[] {0x03});
        assertEquals(BigInteger.ONE, id.getValueAsBigInteger());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateOverflow2() {
        LinearId id = new LinearId(new byte[]{0x76, 0x51},
                new byte[] {(byte) 0x0F, (byte) 0xFF});
        assertEquals(new BigInteger("1617"), id.getValueAsBigInteger());
    }

    @Test
    public void testCreateUnderflow() {
        LinearId id = new LinearId(new byte[]{0x76, 0x51},
                new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
        assertEquals(new BigInteger("30289"), id.getValueAsBigInteger());
    }

    @Test
    public void testAdd1() {
        LinearId id1 = new LinearId(new byte[]{0x01}, new byte[]{0x03});
        LinearId id2 = new LinearId(new byte[]{0x02}, new byte[]{0x03});
        LinearId addedId = id1.add(id2);
        assertEquals(new BigInteger("3"), addedId.getValueAsBigInteger());
    }

    @Test
    public void testAdd2() {
        LinearId id1 = new LinearId(new byte[]{0x01}, new byte[]{0x03});
        LinearId id2 = new LinearId(new byte[]{0x03}, new byte[]{0x03});
        LinearId addedId = id1.add(id2);
        assertEquals(new BigInteger("0"), addedId.getValueAsBigInteger());
    }

    @Test
    public void testAdd3() {
        LinearId id1 = new LinearId(new byte[]{0x02}, new byte[]{0x03});
        LinearId id2 = new LinearId(new byte[]{0x03}, new byte[]{0x03});
        LinearId addedId = id1.add(id2);
        assertEquals(new BigInteger("1"), addedId.getValueAsBigInteger());
    }

    @Test
    public void testAdd4() {
        LinearId id1 = new LinearId(new byte[]{(byte) 0x03}, new byte[]{0x03});
        LinearId id2 = new LinearId(new byte[]{(byte) 0x03}, new byte[]{0x03});
        LinearId addedId = id1.add(id2);
        assertEquals(new BigInteger("2"), addedId.getValueAsBigInteger());
    }

    @Test
    public void testSubtract1() {
        LinearId id1 = new LinearId(new byte[]{0x03}, new byte[]{0x03});
        LinearId id2 = new LinearId(new byte[]{0x01}, new byte[]{0x03});
        LinearId addedId = id1.subtract(id2);
        assertEquals(new BigInteger("2"), addedId.getValueAsBigInteger());
    }

    @Test
    public void testSubtract2() {
        LinearId id1 = new LinearId(new byte[]{0x01}, new byte[]{0x03});
        LinearId id2 = new LinearId(new byte[]{0x02}, new byte[]{0x03});
        LinearId addedId = id1.subtract(id2);
        assertEquals(new BigInteger("3"), addedId.getValueAsBigInteger());
    }

    @Test
    public void testSubtract3() {
        LinearId id1 = new LinearId(new byte[]{0x01}, new byte[]{0x03});
        LinearId id2 = new LinearId(new byte[]{0x03}, new byte[]{0x03});
        LinearId addedId = id1.subtract(id2);
        assertEquals(new BigInteger("2"), addedId.getValueAsBigInteger());
    }

    @Test
    public void testSubtract4() {
        LinearId id1 = new LinearId(new byte[]{(byte) 0x03}, new byte[]{0x03});
        LinearId id2 = new LinearId(new byte[]{(byte) 0x03}, new byte[]{0x03});
        LinearId addedId = id1.subtract(id2);
        assertEquals(new BigInteger("0"), addedId.getValueAsBigInteger());
    }

    @Test
    public void testSubtract5() {
        LinearId id1 = new LinearId(new byte[]{0x01, (byte) 0xFF},
                new byte[]{0x01, (byte) 0xFF});
        LinearId id2 = new LinearId(new byte[]{0x00, (byte) 0xFF},
                new byte[]{0x01, (byte) 0xFF});
        LinearId addedId = id1.subtract(id2);
        assertEquals(new BigInteger("256"), addedId.getValueAsBigInteger());
    }

    @Test
    public void testComparePosition1() {
        LinearId baseId = new LinearId(new byte[]{0x01}, new byte[]{0x03});
        LinearId id1 = new LinearId(new byte[]{0x00}, new byte[]{0x03});
        LinearId id2 = new LinearId(new byte[]{0x02}, new byte[]{0x03});
        int compResult = id1.comparePosition(baseId, id2);
        assertEquals(1, compResult);
    }

    @Test
    public void testComparePosition2() {
        LinearId baseId = new LinearId(new byte[]{0x01}, new byte[]{0x03});
        LinearId id1 = new LinearId(new byte[]{0x02}, new byte[]{0x03});
        LinearId id2 = new LinearId(new byte[]{0x00}, new byte[]{0x03});
        int compResult = id1.comparePosition(baseId, id2);
        assertEquals(-1, compResult);
    }

    @Test
    public void testComparePosition3() {
        LinearId baseId = new LinearId(new byte[]{0x01}, new byte[]{0x03});
        LinearId id1 = new LinearId(new byte[]{0x02}, new byte[]{0x03});
        LinearId id2 = new LinearId(new byte[]{0x02}, new byte[]{0x03});
        int compResult = id1.comparePosition(baseId, id2);
        assertEquals(0, compResult);
    }
    
    @Test
    public void testIsWithin1() {
        LinearId testId = new LinearId(new byte[]{0x02}, new byte[]{0x03});
        LinearId startId = new LinearId(new byte[]{0x01}, new byte[]{0x03});
        LinearId endId = new LinearId(new byte[]{0x03}, new byte[]{0x03});
        
        boolean res;
        
        res = testId.isWithin(startId, true, endId, true);
        assertEquals(res, true);
        res = testId.isWithin(startId, true, endId, false);
        assertEquals(res, true);
        res = testId.isWithin(startId, false, endId, true);
        assertEquals(res, true);
        res = testId.isWithin(startId, false, endId, false);
        assertEquals(res, true);
    }

    @Test
    public void testIsWithin2() {
        LinearId testId = new LinearId(new byte[]{0x01}, new byte[]{0x03});
        LinearId startId = new LinearId(new byte[]{0x01}, new byte[]{0x03});
        LinearId endId = new LinearId(new byte[]{0x03}, new byte[]{0x03});
        
        boolean res;
        
        res = testId.isWithin(startId, true, endId, true);
        assertEquals(res, true);
        res = testId.isWithin(startId, true, endId, false);
        assertEquals(res, true);
        res = testId.isWithin(startId, false, endId, true);
        assertEquals(res, false);
        res = testId.isWithin(startId, false, endId, false);
        assertEquals(res, false);
    }

    @Test
    public void testIsWithin3() {
        LinearId testId = new LinearId(new byte[]{0x03}, new byte[]{0x03});
        LinearId startId = new LinearId(new byte[]{0x01}, new byte[]{0x03});
        LinearId endId = new LinearId(new byte[]{0x03}, new byte[]{0x03});
        
        boolean res;
        
        res = testId.isWithin(startId, true, endId, true);
        assertEquals(res, true);
        res = testId.isWithin(startId, true, endId, false);
        assertEquals(res, false);
        res = testId.isWithin(startId, false, endId, true);
        assertEquals(res, true);
        res = testId.isWithin(startId, false, endId, false);
        assertEquals(res, false);
    }

    @Test
    public void testIsWithin4() {
        LinearId testId = new LinearId(new byte[]{0x00}, new byte[]{0x03});
        LinearId startId = new LinearId(new byte[]{0x01}, new byte[]{0x03});
        LinearId endId = new LinearId(new byte[]{0x03}, new byte[]{0x03});
        
        boolean res;
        
        res = testId.isWithin(startId, true, endId, true);
        assertEquals(res, false);
        res = testId.isWithin(startId, true, endId, false);
        assertEquals(res, false);
        res = testId.isWithin(startId, false, endId, true);
        assertEquals(res, false);
        res = testId.isWithin(startId, false, endId, false);
        assertEquals(res, false);
    }

    @Test
    public void testIsWithin5() {
        LinearId testId = new LinearId(new byte[]{0x03}, new byte[]{0x03});
        LinearId startId = new LinearId(new byte[]{0x02}, new byte[]{0x03});
        LinearId endId = new LinearId(new byte[]{0x00}, new byte[]{0x03});
        
        boolean res;
        
        res = testId.isWithin(startId, true, endId, true);
        assertEquals(res, true);
        res = testId.isWithin(startId, true, endId, false);
        assertEquals(res, true);
        res = testId.isWithin(startId, false, endId, true);
        assertEquals(res, true);
        res = testId.isWithin(startId, false, endId, false);
        assertEquals(res, true);
    }

    @Test
    public void testIsWithin6() {
        LinearId testId = new LinearId(new byte[]{0x02}, new byte[]{0x03});
        LinearId startId = new LinearId(new byte[]{0x02}, new byte[]{0x03});
        LinearId endId = new LinearId(new byte[]{0x00}, new byte[]{0x03});
        
        boolean res;
        
        res = testId.isWithin(startId, true, endId, true);
        assertEquals(res, true);
        res = testId.isWithin(startId, true, endId, false);
        assertEquals(res, true);
        res = testId.isWithin(startId, false, endId, true);
        assertEquals(res, false);
        res = testId.isWithin(startId, false, endId, false);
        assertEquals(res, false);
    }

    @Test
    public void testIsWithin7() {
        LinearId testId = new LinearId(new byte[]{0x00}, new byte[]{0x03});
        LinearId startId = new LinearId(new byte[]{0x02}, new byte[]{0x03});
        LinearId endId = new LinearId(new byte[]{0x00}, new byte[]{0x03});
        
        boolean res;
        
        res = testId.isWithin(startId, true, endId, true);
        assertEquals(res, true);
        res = testId.isWithin(startId, true, endId, false);
        assertEquals(res, false);
        res = testId.isWithin(startId, false, endId, true);
        assertEquals(res, true);
        res = testId.isWithin(startId, false, endId, false);
        assertEquals(res, false);
    }

    @Test
    public void testIsWithin8() {
        LinearId testId = new LinearId(new byte[]{0x01}, new byte[]{0x03});
        LinearId startId = new LinearId(new byte[]{0x02}, new byte[]{0x03});
        LinearId endId = new LinearId(new byte[]{0x00}, new byte[]{0x03});
        
        boolean res;
        
        res = testId.isWithin(startId, true, endId, true);
        assertEquals(res, false);
        res = testId.isWithin(startId, true, endId, false);
        assertEquals(res, false);
        res = testId.isWithin(startId, false, endId, true);
        assertEquals(res, false);
        res = testId.isWithin(startId, false, endId, false);
        assertEquals(res, false);
    }
}
