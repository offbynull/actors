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

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;

/**
 * An address.
 * <p>
 * An address contains one or more address elements. Address elements are strings limited to printable ASCII characters.
 * <p>
 * This class is immutable.
 * @author Kasra Faghihi
 */
public final class Address implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private static final char DELIM = ':';
    private static final char ESCAPE = '\\';

    private final UnmodifiableList<String> addressElements;

    /**
     * Converts an escaped address string back in to an {@link Address}. Pass the result of {@link #toString() } in to this method to
     * recreate the address.
     * <p>
     * Addresses in text form must only contain printable US-ASCII characters and are delimited by colons ({@code ':'}). For example ...
     * <ul>
     * <li>{@code Address.of("one:two")} would produce an address with the elements {@code ["one", "two"]}.</li>
     * <li>{@code Address.of("one\:two")} would produce an address with the elements {@code ["one:two"]}.</li>
     * <li>{@code Address.of("one\\two")} would produce an address with the elements {@code ["one\two"]}.</li>
     * <li>{@code Address.of("::")} would produce an address with the elements {@code ["", "", ""]}.</li>
     * <li>{@code Address.of("")} would produce an address with the element {@code []}.</li>
     * <li>{@code Address.of("a\")} would be invalid (bad escape sequence).</li>
     * <li>{@code Address.of("\a")} would be invalid (bad escape sequence).</li>
     * </ul>
     * @param textAddress address in text-form
     * @return new address
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code textAddress} is malformed (not properly escaped or not printable US-ASCII)
     */
    public static Address fromString(String textAddress) {
        Validate.notNull(textAddress);
        
        List<String> elements = new ArrayList<>();
        try (StringReader reader = new StringReader(textAddress)) {
            while (true) {
                String element = readAndUnescapeNextElement(reader);
                if (element == null) {
                    break;
                }
                elements.add(element);
            }
        } catch (IOException ioe) {
            // this should never happen
            throw new IllegalStateException(ioe);
        }
        return new Address(elements);
    }

    /**
     * Converts a list of strings to an {@link Address}.
     *
     * @param elements list of printable US-ASCII strings
     * @return new address
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if any element in {@code elements} is malformed (not printable US-ASCII)
     */
    public static Address of(List<String> elements) {
        Validate.notNull(elements);
        Validate.noNullElements(elements);
        elements.stream().forEach(// is US-ASCII
                x -> x.chars().forEach(
                        // this should cause surrogate pairs to fail as well, which is what we want!
                        ch -> Validate.isTrue(ch >= 0x20 && ch < 0x7F, "Not printable ASCII") 
                )
        );
        return new Address(new ArrayList<>(elements));
    }

    /**
     * Converts an array of strings to an {@link Address}.
     *
     * @param elements list of printable US-ASCII strings
     * @return new address
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if any element in {@code elements} is malformed (not printable US-ASCII), or if {@code offset} is
     * {@code < 0 || > elements.length}
     */
    public static Address of(String ... elements) {
        Validate.notNull(elements);
        Validate.noNullElements(elements);

        return of(Arrays.asList(elements));
    }

    private static String escapeElement(String element) { // only escapes the delimiter -- ':'
        Validate.notNull(element);

        StringBuilder stringBuilder = new StringBuilder();

        // http://stackoverflow.com/a/3585791   printable ASCII check
        char[] elementChars = element.toCharArray();
        for (char ch : elementChars) {
            Validate.isTrue(ch >= 0x20 && ch < 0x7F, "Not printable ASCII"); // this should cause surrogate pairs to fail as well, which is
            // what we want!
            if (ch == DELIM) {
                stringBuilder.append(ESCAPE).append(DELIM);
            } else if (ch == ESCAPE) {
                stringBuilder.append(ESCAPE).append(ESCAPE);
            } else {
                stringBuilder.append(ch);
            }
        }

        return stringBuilder.toString();
    }

    private static String readAndUnescapeNextElement(StringReader reader) throws IOException { // "\:" -> ":" and "\\" -> "\"
        Validate.notNull(reader);

        StringBuilder stringBuilder = new StringBuilder();

        // http://stackoverflow.com/a/3585791   printable ASCII check
        int readCount = 0;
        boolean escapeMode = false;
        int ch;
        while ((ch = reader.read()) != -1) {
            readCount++;
            Validate.isTrue(ch >= 0x20 && ch < 0x7F, "Not printable ASCII"); // this should cause surrogate pairs to fail as well, which is
            // what we want!

            if (escapeMode) {
                if (ch == DELIM) {
                    stringBuilder.append(DELIM);
                } else if (ch == ESCAPE) {
                    stringBuilder.append(ESCAPE);
                } else {
                    throw new IllegalArgumentException("Unrecognized escape sequence: " + (char) ch);
                }
                
                escapeMode = false;
            } else {
                if (ch == ESCAPE) {
                    // This is the start of an escape sequence. Character after this one will determine what will be dumped.
                    escapeMode = true;
                    continue;
                } else if (ch == DELIM) {
                    // We're unescaping an address element. Encounter a non-escaped separator (colon) is the end of the address element.
                    break;
                }

                stringBuilder.append((char) ch);
            }
        }

        return readCount == 0 ? null : stringBuilder.toString();
    }

    private Address(List<String> addressElements) {
        this.addressElements = (UnmodifiableList<String>) UnmodifiableList.unmodifiableList(addressElements);
    }

    /**
     * Gets the number of elements that make up this address.
     * @return number of elements that make up this address
     */
    public int size() {
        return addressElements.size();
    }

    /**
     * Gets if this address is empty.
     * @return {@code true} if empty, otherwise {@code false}
     */
    public boolean isEmpty() {
        return addressElements.isEmpty();
    }
    
    /**
     * Get elements that make up this address.
     * @return elements that make up this address
     */
    public List<String> getElements() {
        return new ArrayList<>(addressElements);
    }

    /**
     * Get a element at {@code idx} from the list of elements that make up this address.
     * @param idx element index
     * @return element at {@code idx}
     * @throws IllegalArgumentException if {@code idx} is negative or greater than the number of elements that make up this address 
     */
    public String getElement(int idx) {
        Validate.isTrue(idx >= 0 && idx < addressElements.size());
        return addressElements.get(idx);
    }
    
    /**
     * Adds elements to the end of this address. Equivalent to calling {@code appendSuffix(Address.of(elements)}.
     * @param elements elements to appendSuffix
     * @return copy of this address with {@code elements} appended
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    public Address appendSuffix(String ... elements) {
        Validate.notNull(elements);
        Validate.noNullElements(elements);
        return appendSuffix(Address.of(elements));
    }

    /**
     * Adds elements to the end of this address.
     * @param child child to add appendSuffix
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @return copy of this address with {@code child} appended
     */
    public Address appendSuffix(Address child) {
        Validate.notNull(child);
        
        List<String> newElements = new ArrayList<>(addressElements);
        newElements.addAll(child.addressElements);
        
        return new Address(newElements);
    }

    /**
     * Returns {@code true} if this address is a prefix of {@code other} or if this address is equal to {@code other}. Otherwise returns
     * {@code false}.
     * <p>
     * For example...
     * {@code isParentOf(Address.of("one", "two"), Address.of("one", "two", "three"))} returns {@code true}
     * {@code isParentOf(Address.of("one"), Address.of("one", "two", "three"))} returns {@code true}
     * {@code isParentOf(Address.of("one", "two", "three"), Address.of("one", "two", "three"))} returns {@code true}
     * {@code isParentOf(Address.of("one", "two", "three", "four"), Address.of("one", "two", "three"))} returns {@code false}
     * {@code isParentOf(Address.of(""), Address.of("one", "two", "three"))} returns {@code false}
     * {@code isParentOf(Address.of("xxxxx", "two"), Address.of("one", "two", "three"))} returns {@code false}
     * {@code isParentOf(Address.of("one", "xxxxx"), Address.of("one", "two", "three"))} returns {@code false}
     * @param other address to check against
     * @return {@code true} if this address is a prefix of {@code child}, {@code false} otherwise
     * @throws NullPointerException if any argument is {@code null}
     */
    public boolean isPrefixOf(Address other) {
        Validate.notNull(other);
        return other.addressElements.subList(0, addressElements.size()).equals(addressElements);
    }
    
    /**
     * Removes elements in {@code prefix} from the beginning of this address.
     * <p>
     * For example ...
     * <ul>
     * <li>{@code Address.of("one", "two").removeParent("one")} will return {@code Address.of("two")}.</li>
     * <li>{@code Address.of("one", "two").removeParent("one", "two")} will return {@code Address.of()}.</li>
     * <li>{@code Address.of("xxx").removeParent("one", "two")} will throw an exception.</li>
     * </ul>
     * @param prefix address to remove from the beginning of this address
     * @return copy of this address with the last {@code parent} removed from the beginning
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if this address does not start with {@code parent}
     */
    public Address removePrefix(Address prefix) {
        Validate.notNull(prefix);
        Validate.isTrue(prefix.isPrefixOf(this));
        
        // Need to create a new ArrayList instead of passing in subList directly because subList generates a non-serializable list
        List<String> subList = new ArrayList<>(addressElements.subList(prefix.addressElements.size(), addressElements.size()));
        return new Address(subList);
    }
    
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + Objects.hashCode(this.addressElements);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Address other = (Address) obj;
        if (!Objects.equals(this.addressElements, other.addressElements)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        if (addressElements.isEmpty()) {
            return "";
        }
        
        StringJoiner joiner = new StringJoiner(String.valueOf(DELIM));
        addressElements.stream().forEach((element) -> joiner.add(escapeElement(element)));
        return joiner.toString();
    }
}
