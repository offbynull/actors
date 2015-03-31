package com.offbynull.peernetic.core.common;

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
    
    public static final String relativize(String parentAddress, String absoluteAddress) {
        Validate.notNull(parentAddress);
        Validate.notNull(absoluteAddress);
        Validate.isTrue(absoluteAddress.startsWith(parentAddress));
        
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

    public static String removePrefix(String address, int idx) {
        String[] elements = splitAddress(address);
        return getAddress(Arrays.asList(elements).subList(1, elements.length));
    }
    
    public static String getAddressElement(String address, int idx) {
        Validate.notNull(address);
        Validate.isTrue(idx >= 0);
        
        String[] elements = splitAddress(address);
        
        Validate.validIndex(elements, idx);
        
        return elements[idx];
    }

    public static int getIdElementSize(String address) {
        Validate.notNull(address);
        
        return splitAddress(address).length;
    }

    public static String getAddress(String ... ids) {
        Validate.notNull(ids);
        Validate.noNullElements(ids);
        
        StringJoiner joiner = new StringJoiner(SEPARATOR);
        Arrays.stream(ids).forEach(x -> joiner.add(x));
        
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

        return elements.toArray(new String[elements.size()]);
    }

}
