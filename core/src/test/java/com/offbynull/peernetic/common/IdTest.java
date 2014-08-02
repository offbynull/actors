package com.offbynull.peernetic.common;

import java.math.BigInteger;
import static org.junit.Assert.*;
import org.junit.Test;

public final class IdTest {

    @Test
    public void testCreateNormal1() {
        Id id = new Id(new byte[]{0x01}, new byte[]{0x03});
        assertEquals(BigInteger.ONE, id.getValueAsBigInteger());
    }

    @Test
    public void testCreateNormal2() {
        Id id = new Id(new byte[]{0x11}, new byte[]{(byte) 0xFF});
        assertEquals(new BigInteger("17"), id.getValueAsBigInteger());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNormal3() {
        Id id = new Id(new byte[]{0x11}, new byte[0]);
        assertEquals(new BigInteger("0"), id.getValueAsBigInteger());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateOverflow1() {
        Id id = new Id(new byte[]{0x76, 0x51}, new byte[] {0x03});
        assertEquals(BigInteger.ONE, id.getValueAsBigInteger());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateOverflow2() {
        Id id = new Id(new byte[]{0x76, 0x51},
                new byte[] {(byte) 0x0F, (byte) 0xFF});
        assertEquals(new BigInteger("1617"), id.getValueAsBigInteger());
    }

    @Test
    public void testCreateUnderflow() {
        Id id = new Id(new byte[]{0x76, 0x51},
                new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
        assertEquals(new BigInteger("30289"), id.getValueAsBigInteger());
    }

    @Test
    public void testAdd1() {
        Id id1 = new Id(new byte[]{0x01}, new byte[]{0x03});
        Id id2 = new Id(new byte[]{0x02}, new byte[]{0x03});
        Id addedId = Id.add(id1, id2);
        assertEquals(new BigInteger("3"), addedId.getValueAsBigInteger());
    }

    @Test
    public void testAdd2() {
        Id id1 = new Id(new byte[]{0x01}, new byte[]{0x03});
        Id id2 = new Id(new byte[]{0x03}, new byte[]{0x03});
        Id addedId = Id.add(id1, id2);
        assertEquals(new BigInteger("0"), addedId.getValueAsBigInteger());
    }

    @Test
    public void testAdd3() {
        Id id1 = new Id(new byte[]{0x02}, new byte[]{0x03});
        Id id2 = new Id(new byte[]{0x03}, new byte[]{0x03});
        Id addedId = Id.add(id1, id2);
        assertEquals(new BigInteger("1"), addedId.getValueAsBigInteger());
    }

    @Test
    public void testAdd4() {
        Id id1 = new Id(new byte[]{(byte) 0x03}, new byte[]{0x03});
        Id id2 = new Id(new byte[]{(byte) 0x03}, new byte[]{0x03});
        Id addedId = Id.add(id1, id2);
        assertEquals(new BigInteger("2"), addedId.getValueAsBigInteger());
    }

    @Test
    public void testSubtract1() {
        Id id1 = new Id(new byte[]{0x03}, new byte[]{0x03});
        Id id2 = new Id(new byte[]{0x01}, new byte[]{0x03});
        Id addedId = Id.subtract(id1, id2);
        assertEquals(new BigInteger("2"), addedId.getValueAsBigInteger());
    }

    @Test
    public void testSubtract2() {
        Id id1 = new Id(new byte[]{0x01}, new byte[]{0x03});
        Id id2 = new Id(new byte[]{0x02}, new byte[]{0x03});
        Id addedId = Id.subtract(id1, id2);
        assertEquals(new BigInteger("3"), addedId.getValueAsBigInteger());
    }

    @Test
    public void testSubtract3() {
        Id id1 = new Id(new byte[]{0x01}, new byte[]{0x03});
        Id id2 = new Id(new byte[]{0x03}, new byte[]{0x03});
        Id addedId = Id.subtract(id1, id2);
        assertEquals(new BigInteger("2"), addedId.getValueAsBigInteger());
    }

    @Test
    public void testSubtract4() {
        Id id1 = new Id(new byte[]{(byte) 0x03}, new byte[]{0x03});
        Id id2 = new Id(new byte[]{(byte) 0x03}, new byte[]{0x03});
        Id addedId = Id.subtract(id1, id2);
        assertEquals(new BigInteger("0"), addedId.getValueAsBigInteger());
    }

    @Test
    public void testSubtract5() {
        Id id1 = new Id(new byte[]{0x01, (byte) 0xFF},
                new byte[]{0x01, (byte) 0xFF});
        Id id2 = new Id(new byte[]{0x00, (byte) 0xFF},
                new byte[]{0x01, (byte) 0xFF});
        Id addedId = Id.subtract(id1, id2);
        assertEquals(new BigInteger("256"), addedId.getValueAsBigInteger());
    }

    @Test
    public void testComparePosition1() {
        Id baseId = new Id(new byte[]{0x01}, new byte[]{0x03});
        Id id1 = new Id(new byte[]{0x00}, new byte[]{0x03});
        Id id2 = new Id(new byte[]{0x02}, new byte[]{0x03});
        int compResult = Id.comparePosition(baseId, id1, id2);
        assertEquals(1, compResult);
    }

    @Test
    public void testComparePosition2() {
        Id baseId = new Id(new byte[]{0x01}, new byte[]{0x03});
        Id id1 = new Id(new byte[]{0x02}, new byte[]{0x03});
        Id id2 = new Id(new byte[]{0x00}, new byte[]{0x03});
        int compResult = Id.comparePosition(baseId, id1, id2);
        assertEquals(-1, compResult);
    }

    @Test
    public void testComparePosition3() {
        Id baseId = new Id(new byte[]{0x01}, new byte[]{0x03});
        Id id1 = new Id(new byte[]{0x02}, new byte[]{0x03});
        Id id2 = new Id(new byte[]{0x02}, new byte[]{0x03});
        int compResult = Id.comparePosition(baseId, id1, id2);
        assertEquals(0, compResult);
    }
    
    @Test
    public void testIsWithin1() {
        Id testId = new Id(new byte[]{0x02}, new byte[]{0x03});
        Id startId = new Id(new byte[]{0x01}, new byte[]{0x03});
        Id endId = new Id(new byte[]{0x03}, new byte[]{0x03});
        
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
        Id testId = new Id(new byte[]{0x01}, new byte[]{0x03});
        Id startId = new Id(new byte[]{0x01}, new byte[]{0x03});
        Id endId = new Id(new byte[]{0x03}, new byte[]{0x03});
        
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
        Id testId = new Id(new byte[]{0x03}, new byte[]{0x03});
        Id startId = new Id(new byte[]{0x01}, new byte[]{0x03});
        Id endId = new Id(new byte[]{0x03}, new byte[]{0x03});
        
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
        Id testId = new Id(new byte[]{0x00}, new byte[]{0x03});
        Id startId = new Id(new byte[]{0x01}, new byte[]{0x03});
        Id endId = new Id(new byte[]{0x03}, new byte[]{0x03});
        
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
        Id testId = new Id(new byte[]{0x03}, new byte[]{0x03});
        Id startId = new Id(new byte[]{0x02}, new byte[]{0x03});
        Id endId = new Id(new byte[]{0x00}, new byte[]{0x03});
        
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
        Id testId = new Id(new byte[]{0x02}, new byte[]{0x03});
        Id startId = new Id(new byte[]{0x02}, new byte[]{0x03});
        Id endId = new Id(new byte[]{0x00}, new byte[]{0x03});
        
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
        Id testId = new Id(new byte[]{0x00}, new byte[]{0x03});
        Id startId = new Id(new byte[]{0x02}, new byte[]{0x03});
        Id endId = new Id(new byte[]{0x00}, new byte[]{0x03});
        
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
        Id testId = new Id(new byte[]{0x01}, new byte[]{0x03});
        Id startId = new Id(new byte[]{0x02}, new byte[]{0x03});
        Id endId = new Id(new byte[]{0x00}, new byte[]{0x03});
        
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
