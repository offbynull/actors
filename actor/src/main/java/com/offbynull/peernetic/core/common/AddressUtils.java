package com.offbynull.peernetic.core.common;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import org.apache.commons.lang3.Validate;

public final class AddressUtils {

    public static final String SEPARATOR = ":";

    private AddressUtils() {
        // do nothing
    }

    public static String getPrefix(String address) {
        return splitAddress(address).getPrefix();
    }

    public static String getId(String address) {
        return splitAddress(address).getId();
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

    public static String getIdElement(String address, int idx) {
        return splitAddress(address).getIdElement(idx);
    }

    public static int getIdElementSize(String address) {
        return splitAddress(address).getIdElementSize();
    }

    public static String getAddress(String prefix, String id) {
        Validate.notNull(prefix);
        Validate.notNull(id);
        return getAddress(prefix, Collections.singletonList(id));
    }
    
    public static String getAddress(String prefix, List<String> idElements) {
        Validate.notNull(prefix);
        Validate.notNull(idElements);
        Validate.notEmpty(prefix);
        Validate.noNullElements(idElements);
        
        StringJoiner joiner = new StringJoiner(SEPARATOR);
        joiner.add(prefix);
        idElements.forEach(x -> joiner.add(x));
        
        return joiner.toString();
    }

    public static String getAddress(Address actorAddress) {
        Validate.notNull(actorAddress);
        return getAddress(actorAddress.prefix, actorAddress.idElements);
    }

    public static Address splitAddress(String address) {
        // get prefix, prefix must always end with a separator, so even if there are no id elements it'd be something like "prefix:"
        int separatorIdx = address.indexOf(SEPARATOR);
        Validate.isTrue(separatorIdx != -1);
        String prefix = address.substring(0, separatorIdx);
        
        // add stuff after prefix
        int startIdx = separatorIdx;
        int endIdx;
        List<String> idElements = new LinkedList<>();
        while ((endIdx = address.indexOf(SEPARATOR, startIdx + 1)) != -1) {
            String idElement = address.substring(startIdx + 1, endIdx);
            idElements.add(idElement);
            startIdx = endIdx;
        }
        
        // add tail element, if it exists
        if (startIdx < address.length()) {
            String idElement = address.substring(startIdx + 1);
            idElements.add(idElement);
        }

        return new Address(prefix, idElements);
    }

}
