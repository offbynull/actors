/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.core.shuttle;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import org.apache.commons.lang3.Validate;

/**
 * Utility methods for addresses.
 * @author Kasra Faghihi
 */
public final class AddressUtils {

    public static final String SEPARATOR = ":";

    private AddressUtils() {
        // do nothing
    }
    
    /**
     * Checks if one address is a prefix of the other.
     * @param parentAddress address to check against (the prefix)
     * @param absoluteAddress address to check
     * @return {@code true} if {@code parentAddress} is a prefix of {@code absoluteAddress}
     * @throws NullPointerException if any argument is {@code null}
     */
    public static final boolean isPrefix(String parentAddress, String absoluteAddress) {
        Validate.notNull(parentAddress);
        Validate.notNull(absoluteAddress);
        
        // if address being tested is smaller than parent, fail
        if (parentAddress.length() > absoluteAddress.length()) {
            return false;
        }
        
        // if address being tested is the same size as parent, make sure strings are equal
        if (parentAddress.length() == absoluteAddress.length() && parentAddress.equals(absoluteAddress)) {
            return true;
        }
        
        // otherwise, return true if other starts with parent and has a : right after it
        return absoluteAddress.startsWith(parentAddress + SEPARATOR);
    }

    /**
     * Strips off the prefix from an address.
     * @param parentAddress address to check for (the prefix)
     * @param absoluteAddress address to strip prefix from
     * @return {@code absoluteAddress} with the prefix {@code parentAddress} stripped off
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code parentAddress} is not a prefix of {@code absoluteAddress}
     */
    public static final String relativize(String parentAddress, String absoluteAddress) {
        Validate.notNull(parentAddress);
        Validate.notNull(absoluteAddress);
        Validate.isTrue(absoluteAddress.startsWith(parentAddress), "%s is not a child of %s", absoluteAddress, parentAddress);
        
        String ret = absoluteAddress.substring(parentAddress.length());
        Validate.isTrue(ret.startsWith(SEPARATOR), "%s is not a child of %s", absoluteAddress, parentAddress);
        ret = ret.substring(1);
        
        return ret;
    }

    /**
     * Adds a prefix to an address.
     * @param parentAddress prefix to append
     * @param relativeAddress address to add prefix to
     * @return {@code relativeAddress} with {@code parentAddress} prefixed on to it
     * @throws NullPointerException if any argument is {@code null}
     */
    public static final String parentize(String parentAddress, String relativeAddress) {
        Validate.notNull(parentAddress);
        Validate.notNull(relativeAddress);
        
        return parentAddress + SEPARATOR + relativeAddress;
    }

    /**
     * Removes a number of address elements from the end of an address. For example, removing {@code 2} address elements from
     * {@code "test1:test2:test3:test4"} will result in {@code "test1:test2"}.
     * @param address address to remove address elements from
     * @param removeCount number of address elements to remove from the bottom
     * @return {@code address} with the last {@code removeCount} address elements removed
     * @throws NullPointerException if any argument is {@code null}
     * @throws IndexOutOfBoundsException if the number of address elements in {@code address} is less than {@code removeCount}
     */
    public static String removeSuffix(String address, int removeCount) {
        Validate.notNull(address);
        
        String[] elements = splitAddress(address);
        Validate.validIndex(elements, removeCount);
        return getAddress(Arrays.asList(elements).subList(0, elements.length - removeCount));
    }
    
    /**
     * Get the address element at a certain index. For example, getting address element at index {@code 2} from
     * {@code "test1:test2:test3:test4"} will result in {@code "test3"}.
     * @param address address
     * @param idx index of address element
     * @return {@code address}'s {@code idx} address element
     * @throws NullPointerException if any argument is {@code null}
     * @throws IndexOutOfBoundsException if the number of address elements in {@code address} is less than {@code idx}
     */
    public static String getElement(String address, int idx) {
        Validate.notNull(address);
        
        String[] elements = splitAddress(address);
        
        Validate.validIndex(elements, idx);
        
        return elements[idx];
    }
    
    /**
     * Get the number of address elements in an address. For example, getting the number of address elements in
     * {@code "test1:test2:test3:test4"} will result in {@code 4}
     * @param address address
     * @return number of address elements in {@code address}
     * @throws NullPointerException if any argument is {@code address}
     */
    public static int getElementSize(String address) {
        Validate.notNull(address);
        
        return splitAddress(address).length;
    }

    /**
     * Generates an address from an array of address elements.
     * @param ids address elements to concatenate
     * @return address consisting of elements from {@code ids} for its address elements
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if {@code ids} is empty
     */
    public static String getAddress(String ... ids) {
        return getAddress(0, ids);
    }

    /**
     * Generates an address from an array of address elements.
     * @param offset offset to begin reading address elements from {@code ids} from
     * @param ids address elements to concatenate
     * @return address consisting of elements from {@code ids} for its address elements
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if {@code offset < 0 || offset >= ids.length}, or if an element in {@code ids} contains {@code :}
     */
    public static String getAddress(int offset, String ... ids) {
        Validate.notNull(ids);
        Validate.noNullElements(ids);
        Validate.isTrue(offset >= 0);
        Validate.isTrue(offset < ids.length);
        
        StringJoiner joiner = new StringJoiner(SEPARATOR);
        Arrays.asList(ids).subList(offset, ids.length).forEach(x -> {
            Validate.isTrue(!x.contains(":"));
            joiner.add(x);
        });
        
        return joiner.toString();
    }
    
    private static String getAddress(List<String> idElements) {
        Validate.notNull(idElements);
        Validate.noNullElements(idElements);
        
        StringJoiner joiner = new StringJoiner(SEPARATOR);
        idElements.forEach(x -> joiner.add(x));
        
        return joiner.toString();
    }

    /**
     * Splits an address up in to its individual address elements. For example, {@code "test1:test2:test3:test4"} will result in
     * an array of 4 elements: {@code "test1", "test2", "test3", "test4"}.
     * @param address address to split
     * @return individual address elements of {@code address}
     * @throws NullPointerException if any argument is {@code null}
     */
    public static String[] splitAddress(String address) {
        Validate.notNull(address);
        
        int startIdx = 0;
        int endIdx;
        List<String> elements = new LinkedList<>();
        while ((endIdx = address.indexOf(SEPARATOR, startIdx)) != -1) {
            String element = address.substring(startIdx, endIdx);
            elements.add(element);
            startIdx = endIdx + 1;
        }
        
        // add tail element, if it exists
        if (startIdx < address.length() || address.endsWith(SEPARATOR)) {
            String element = address.substring(startIdx);
            elements.add(element);
        }
        
        if (elements.isEmpty()) {
            elements.add("");
        }

        return elements.toArray(new String[elements.size()]);
    }

}
