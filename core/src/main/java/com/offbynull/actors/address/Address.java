/*
 * Copyright (c) 2018, Kasra Faghihi, All rights reserved.
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
package com.offbynull.actors.address;

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
     * <li>{@code Address.fromString("one:two")} would produce an address with the elements {@code ["one", "two"]}.</li>
     * <li>{@code Address.fromString("one\:two")} would produce an address with the elements {@code ["one:two"]}.</li>
     * <li>{@code Address.fromString("one\\two")} would produce an address with the elements {@code ["one\two"]}.</li>
     * <li>{@code Address.fromString("::")} would produce an address with the elements {@code ["", "", ""]}.</li>
     * <li>{@code Address.fromString("")} would produce an address with the element {@code [""]}.</li>
     * <li>{@code Address.fromString("a\")} would be invalid (bad escape sequence).</li>
     * <li>{@code Address.fromString("\a")} would be invalid (bad escape sequence).</li>
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
                elements.add(element);
                
                // Test to make sure not at EOF
                reader.mark(1);
                if (reader.read() == -1) {
                    break;
                }
                reader.reset();
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
     * @throws IllegalArgumentException if {@code elements} is empty, or if any element in {@code elements} is malformed (not printable
     * US-ASCII)
     */
    public static Address of(List<String> elements) {
        Validate.notNull(elements);
        Validate.noNullElements(elements);
        Validate.isTrue(!elements.isEmpty());
        
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
    public static Address of(String... elements) {
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
            switch (ch) {
                case DELIM:
                    stringBuilder.append(ESCAPE).append(DELIM);
                    break;
                case ESCAPE:
                    stringBuilder.append(ESCAPE).append(ESCAPE);
                    break;
                default:
                    stringBuilder.append(ch);
                    break;
            }
        }

        return stringBuilder.toString();
    }

    private static String readAndUnescapeNextElement(StringReader reader) throws IOException { // "\:" -> ":" and "\\" -> "\"
        Validate.notNull(reader);

        StringBuilder stringBuilder = new StringBuilder();

        // http://stackoverflow.com/a/3585791   printable ASCII check
        boolean escapeMode = false;
        int ch;
        while ((ch = reader.read()) != -1) {
            Validate.isTrue(ch >= 0x20 && ch < 0x7F, "Not printable ASCII"); // this should cause surrogate pairs to fail as well, which is
            // what we want!

            if (escapeMode) {
                switch (ch) {
                    case DELIM:
                        stringBuilder.append(DELIM);
                        break;
                    case ESCAPE:
                        stringBuilder.append(ESCAPE);
                        break;
                    default:
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

        return stringBuilder.toString();
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
    public Address appendSuffix(String... elements) {
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
     * {@code isPrefixOf(Address.of("one", "two"), Address.of("one", "two", "three"))} returns {@code true}
     * {@code isPrefixOf(Address.of("one"), Address.of("one", "two", "three"))} returns {@code true}
     * {@code isPrefixOf(Address.of("one", "two", "three"), Address.of("one", "two", "three"))} returns {@code true}
     * {@code isPrefixOf(Address.of("one", "two", "three", "four"), Address.of("one", "two", "three"))} returns {@code false}
     * {@code isPrefixOf(Address.of(""), Address.of("one", "two", "three"))} returns {@code false}
     * {@code isPrefixOf(Address.of("xxxxx", "two"), Address.of("one", "two", "three"))} returns {@code false}
     * {@code isPrefixOf(Address.of("one", "xxxxx"), Address.of("one", "two", "three"))} returns {@code false}
     * @param other address to check against
     * @return {@code true} if this address is a prefix of {@code child}, {@code false} otherwise
     * @throws NullPointerException if any argument is {@code null}
     */
    public boolean isPrefixOf(Address other) {
        Validate.notNull(other);
        
        if (other.addressElements.size() < addressElements.size()) {
            return false;
        }
        
        return other.addressElements.subList(0, addressElements.size()).equals(addressElements);
    }
    
    /**
     * Removes elements in {@code prefix} from the beginning of this address.
     * <p>
     * For example ...
     * <ul>
     * <li>{@code Address.of("one", "two").removePrefix("one")} will return {@code Address.of("two")}.</li>
     * <li>{@code Address.of("xxx").removePrefix("one", "two")} will throw an exception.</li>
     * </ul>
     * @param prefix address to remove from the beginning of this address
     * @return copy of this address with the last {@code parent} removed from the beginning
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if this address does not start with {@code parent}, or if the {@code prefix} is equal to
     * {@code parent} (the result would have no elements)
     */
    public Address removePrefix(Address prefix) {
        Validate.notNull(prefix);
        Validate.isTrue(prefix.isPrefixOf(this));
        
        // Need to create a new ArrayList instead of passing in subList directly because subList generates a non-serializable list
        List<String> subList = new ArrayList<>(addressElements.subList(prefix.addressElements.size(), addressElements.size()));
        Validate.isTrue(!subList.isEmpty());
        return new Address(subList);
    }

    /**
     * Removes a number of address elements from the end of this address. For example, removing {@code 2} address elements from
     * {@code ["test1", "test2", "test3", "test4"]} will result in {@code ["test1", "test2"]}.
     * @param count number of address elements to remove from the tail of this address
     * @return a copy of this address with the last {@code removeCount} address elements removed
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if the number of address elements in this address is less than {@code removeCount}, or if the
     * {@code removeSuffix >= 0 || removeSuffix < this.length} (if {@code removeSuffix == this.length) the result would have no elements)
     */
    public Address removeSuffix(int count) {
        Validate.isTrue(count >= 0 && count < addressElements.size());
        List<String> subList = new ArrayList<>(addressElements.subList(0, addressElements.size() - count));
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
        StringJoiner joiner = new StringJoiner(String.valueOf(DELIM));
        addressElements.stream().forEach((element) -> joiner.add(escapeElement(element)));
        return joiner.toString();
    }
}
