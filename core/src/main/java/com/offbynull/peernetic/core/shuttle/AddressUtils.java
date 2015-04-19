package com.offbynull.peernetic.core.shuttle;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import org.apache.commons.lang3.Validate;

public final class AddressUtils {

    public static final String SEPARATOR = ":";

    private AddressUtils() {
        // do nothing
    }
    
    public static final boolean isParent(String parentAddress, String otherAddress) {
        Validate.notNull(parentAddress);
        Validate.notNull(otherAddress);
        
        // if address being tested is smaller than parent, fail
        if (parentAddress.length() > otherAddress.length()) {
            return false;
        }
        
        // if address being tested is the same size as parent, make sure strings are equal
        if (parentAddress.length() == otherAddress.length() && parentAddress.equals(otherAddress)) {
            return true;
        }
        
        // otherwise, return true if other starts with parent and has a : right after it
        return otherAddress.startsWith(parentAddress + SEPARATOR);
    }

    public static final String relativize(String parentAddress, String absoluteAddress) {
        Validate.notNull(parentAddress);
        Validate.notNull(absoluteAddress);
        Validate.isTrue(absoluteAddress.startsWith(parentAddress), "%s is not a child of %s", absoluteAddress, parentAddress);
        
        String ret = absoluteAddress.substring(parentAddress.length());
        if (ret.startsWith(SEPARATOR)) {
            ret = ret.substring(1);
        }
        
        return ret;
    }

    public static final String parentize(String parentAddress, String relativeAddress) {
        Validate.notNull(parentAddress);
        Validate.notNull(relativeAddress);
        
        return parentAddress + SEPARATOR + relativeAddress;
    }

    public static String removePrefix(String address, int removeCount) {
        String[] elements = splitAddress(address);
        Validate.validIndex(elements, removeCount);
        return getAddress(Arrays.asList(elements).subList(removeCount, elements.length));
    }

    public static String removeSuffix(String address, int removeCount) {
        String[] elements = splitAddress(address);
        Validate.validIndex(elements, removeCount);
        return getAddress(Arrays.asList(elements).subList(0, elements.length - removeCount));
    }
    
    public static String getAddressElement(String address, int idx) {
        Validate.notNull(address);
        Validate.isTrue(idx >= 0);
        
        String[] elements = splitAddress(address);
        
        Validate.validIndex(elements, idx);
        
        return elements[idx];
    }

    public static String getFirstAddressElement(String address) {
        Validate.notNull(address);
        
        String[] elements = splitAddress(address);
        
        return elements[0];
    }

    public static String getLastAddressElement(String address) {
        Validate.notNull(address);
        
        String[] elements = splitAddress(address);
        
        return elements[elements.length - 1];
    }
    
    public static int getIdElementSize(String address) {
        Validate.notNull(address);
        
        return splitAddress(address).length;
    }

    public static String getAddress(String ... ids) {
        return getAddress(0, ids);
    }

    public static String getAddress(int offset, String ... ids) {
        Validate.notNull(ids);
        Validate.noNullElements(ids);
        Validate.isTrue(offset >= 0);
        Validate.isTrue(offset < ids.length);
        
        StringJoiner joiner = new StringJoiner(SEPARATOR);
        Arrays.asList(ids).subList(offset, ids.length).forEach(x -> joiner.add(x));
        
        return joiner.toString();
    }
    
    public static String getAddress(List<String> idElements) {
        Validate.notNull(idElements);
        Validate.noNullElements(idElements);
        
        StringJoiner joiner = new StringJoiner(SEPARATOR);
        idElements.forEach(x -> joiner.add(x));
        
        return joiner.toString();
    }

    public static String[] splitAddress(String address) {
        int startIdx = 0;
        int endIdx;
        List<String> elements = new LinkedList<>();
        while ((endIdx = address.indexOf(SEPARATOR, startIdx)) != -1) {
            String element = address.substring(startIdx, endIdx);
            elements.add(element);
            startIdx = endIdx + 1;
        }
        
        // add tail element, if it exists
        if (startIdx < address.length()) {
            String element = address.substring(startIdx);
            elements.add(element);
        }
        
        if (elements.isEmpty()) {
            elements.add("");
        }

        return elements.toArray(new String[elements.size()]);
    }

}
