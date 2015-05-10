package com.offbynull.peernetic.core.shuttle;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AddressUtilsTest {
    
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void mustConcatenateAddressElements() {
        String address = AddressUtils.getAddress("1", "2", "3");
        assertEquals("1:2:3", address);
    }

    @Test
    public void mustConcatenatePartialAddressElements() {
        String address = AddressUtils.getAddress(2, "1", "2", "3");
        assertEquals("3", address);
    }

    @Test
    public void mustFailWhenNoAddressElementsProvidedForConcatenation() {
        exception.expect(IllegalArgumentException.class);
        AddressUtils.getAddress();
    }
    
    @Test
    public void mustFailConcatenationWhenAddressElementContainsColon() {
        exception.expect(IllegalArgumentException.class);
        AddressUtils.getAddress(":");
    }
    
    @Test
    public void mustGetAddressElementAtIndex() {
        String addressElement = AddressUtils.getElement("1:2:3", 1);
        assertEquals("2", addressElement);
    }

    @Test
    public void mustFailToGetAddressElementAtOutOfBoundsIndex() {
        exception.expect(IndexOutOfBoundsException.class);
        AddressUtils.getElement("1:2:3", 4);
    }

    @Test
    public void mustFailToGetAddressElementAtNegativeIndex() {
        exception.expect(IndexOutOfBoundsException.class);
        AddressUtils.getElement("1:2:3", -1);
    }

    @Test
    public void mustGetAddressElementSize() {
        int size = AddressUtils.getElementSize("1:2:3");
        assertEquals(3, size);
    }

    @Test
    public void mustGetEmptyAddressElementSize() {
        int size = AddressUtils.getElementSize("");
        assertEquals(1, size);
    }

    @Test
    public void mustProperlyIdentifyAsPrefix() {
        boolean res = AddressUtils.isPrefix("1:2", "1:2:3");
        assertTrue(res);
    }

    @Test
    public void mustProperlyIdentifyAsNotAPrefix() {
        boolean res = AddressUtils.isPrefix("1:2", "1:22:3");
        assertFalse(res);
    }
    
    @Test
    public void mustParentizeAddress() {
        String address = AddressUtils.parentize("1:2", "3");
        assertEquals("1:2:3", address);
    }

    @Test
    public void mustParentizeAddressWithEndingColon() {
        String address = AddressUtils.parentize("1:2", "3:");
        assertEquals("1:2:3:", address);
    }
    
    @Test
    public void mustParentizeAddressWithEmptyRelativeAddress() {
        String address = AddressUtils.parentize("1:2", "");
        assertEquals("1:2:", address);
    }

    @Test
    public void mustParentizeAddressWithNullRelativeAddress() {
        String address = AddressUtils.parentize("1:2", null);
        assertEquals("1:2", address);
    }

    @Test
    public void mustParentizeAddressWithEmptyParentAddress() {
        String address = AddressUtils.parentize("", "3");
        assertEquals(":3", address);
    }
    
    @Test
    public void mustRelativizeAddress() {
        String address = AddressUtils.relativize("1", "1:2:3");
        assertEquals("2:3", address);
    }

    @Test
    public void mustRelativizeAddressWithEmptyParent() {
        String address = AddressUtils.relativize("", ":2:3");
        assertEquals("2:3", address);
    }

    @Test
    public void mustRelativizeAddressToNullWhenParentsAreTheSame() {
        String address = AddressUtils.relativize("1:2:3", "1:2:3");
        assertNull(address);
    }

    @Test
    public void mustFailToRelativizeAddressWithIncorrectParent() {
        exception.expect(IllegalArgumentException.class);
        AddressUtils.relativize("1", "11:2:3");
    }

    @Test
    public void mustRemoveSuffix() {
        String address = AddressUtils.removeSuffix("1:2:3", 1);
        assertEquals("1:2", address);
    }

    @Test
    public void mustNotRemoveSuffix() {
        String address = AddressUtils.removeSuffix("1:2:3", 0);
        assertEquals("1:2:3", address);
    }

    @Test
    public void mustFailToRemoveSuffixIfOutOfBounds() {
        exception.expect(IndexOutOfBoundsException.class);
        AddressUtils.removeSuffix("1:2:3", 4);
    }

    @Test
    public void mustFailToRemoveSuffixIfNegative() {
        exception.expect(IndexOutOfBoundsException.class);
        AddressUtils.removeSuffix("1:2:3", -1);
    }

    @Test
    public void mustSplitAddress() {
        String[] splitAddress = AddressUtils.splitAddress("1:2:3");
        assertArrayEquals(new String[] {"1", "2", "3"}, splitAddress);
    }

    @Test
    public void mustSplitSingleElemntAddress() {
        String[] splitAddress = AddressUtils.splitAddress("1");
        assertArrayEquals(new String[] {"1"}, splitAddress);
    }

    @Test
    public void mustSplitAddressWithEmptyElemnt() {
        String[] splitAddress = AddressUtils.splitAddress("1:");
        assertArrayEquals(new String[] {"1", ""}, splitAddress);
    }

    @Test
    public void mustResultInSingleEmptyAdressElementWhenSplittingEmptyAddress() {
        String[] splitAddress = AddressUtils.splitAddress("");
        assertArrayEquals(new String[] {""}, splitAddress);
    }
}
