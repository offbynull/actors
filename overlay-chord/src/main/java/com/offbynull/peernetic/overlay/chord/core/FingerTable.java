/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.overlay.chord.core;

import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.IdUtils;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import org.apache.commons.lang3.Validate;

/**
 * Holds on to routing information. For more information on how a finger table
 * operates, please refer to the Chord research paper. This implementation makes
 * some minor additions to the original finger table algorithm to ensure that
 * that there aren't any inconsistencies. That is, this implementation
 * guarantees that...
 * <ol>
 * <li>
 * Fingers that point to the base pointer show up for a contiguous range from
 * the entry after the last non-base entry (or 0 if there are no non-base
 * entries) all the way to the last entry in the finger table. For example:
 * <p/>
 * Imagine that you have just created a finger table and the base id is 0:<br/>
 * [0, 0, 0, 0, 0, 0].
 * <p/>
 * You insert a finger with id 8:<br/>
 * [8, 8, 8, 8, 0, 0].
 * <p/>
 * </li>
 * <li>An inserted finger will propagate backwards until it finds an entry that
 * isn't the base entry and isn't the same id as the id being replaced, but is
 * greater than or equal to the expected id. For example:
 * <p/>
 * Imagine that you have just created a finger table and the base id is 0:<br/>
 * [0, 0, 0, 0, 0, 0].
 * <p/>
 * You insert a finger with id 16:<br/>
 * [16, 16, 16, 16, 16, 0].
 * <p/>
 * You insert a finger with id 2:<br/>
 * [2, 2, 16, 16, 16, 0].
 * <p/>
 * You insert a finger with id 8:<br/>
 * [2, 2, 8, 8, 16, 0].
 * <p/>
 * You insert a finger with id 4:<br/>
 * [2, 2, 4, 8, 16, 0].
 * </li>
 * <li>A finger being removed will propagate backwards until it finds an entry
 * that isn't the base (it will never be base -- based on previous guarantees)
 * and isn't the same id as the id being removed. The replacement value for the
 * finger being removed will be the finger in front of it (or base if it's the
 * last finger and there are no other fingers in front of it). For example:
 * <p/>
 * Imagine that you have just created a finger table and the base id is 0:<br/>
 * [0, 0, 0, 0, 0, 0].
 * <p/>
 * You insert a finger with id 16:<br/>
 * [16, 16, 16, 16, 16, 0].
 * <p/>
 * You insert a finger with id 2:<br/>
 * [2, 2, 16, 16, 16, 0].
 * <p/>
 * You insert a finger with id 8:<br/>
 * [2, 2, 8, 8, 16, 0].
 * <p/>
 * You remove a finger with id 8:<br/>
 * [2, 2, 16, 16, 16, 0].
 * <p/>
 * You remove a finger with id 16:<br/>
 * [2, 2, 0, 0, 0, 0].
 * <p/>
 * You remove a finger with id 2:<br/>
 * [0, 0, 0, 0, 0, 0].
 * </li>
 * </ol>
 *
 * @param <A> address type
 * @author Kasra Faghihi
 */
public final class FingerTable<A> {

    /**
     * Internal table that keeps track of fingers.
     */
    private List<InternalEntry> table;
    /**
     * Base (self) pointer.
     */
    private Pointer<A> basePtr;
    /**
     * The bit count in {@link #basePtr}.
     */
    private int bitCount;

    /**
     * Constructs a {@link FingerTable}. All fingers are initialized to
     * {@code base}.
     *
     * @param basePtr base pointer
     * @throws NullPointerException if any arguments are {@code null}
     */
    public FingerTable(Pointer<A> basePtr) {
        Validate.notNull(basePtr);
        IdUtils.ensureLimitPowerOfTwo(basePtr);

        this.basePtr = basePtr;

        Id baseId = basePtr.getId();
        A baseAddress = basePtr.getAddress();

        this.bitCount = IdUtils.getLimitBitLength(basePtr);
        byte[] limit = baseId.getLimitAsByteArray();

        table = new ArrayList<>(bitCount);
        for (int i = 0; i < bitCount; i++) {
            BigInteger data = BigInteger.ONE.shiftLeft(i);
            byte[] offsetIdRaw = data.toByteArray();
            Id offsetId = new Id(offsetIdRaw, limit);
            Id expectedId = baseId.add(offsetId);

            InternalEntry te = new InternalEntry();
            te.expectedId = expectedId;
            te.actualId = baseId;
            te.address = baseAddress;

            table.add(te);
        }
    }

    /**
     * Constructs a copy of a {@link FingerTable}.
     *
     * @param orig finger table to copy
     * @throws NullPointerException if any arguments are {@code null}
     */
    public FingerTable(FingerTable<A> orig) {
        if (orig == null) {
            throw new NullPointerException();
        }

        basePtr = orig.basePtr;
        bitCount = orig.bitCount;
        table = new ArrayList<>(orig.table.size());
        for (InternalEntry origTe : orig.table) {
            InternalEntry te = new InternalEntry();
            te.expectedId = origTe.expectedId;
            te.actualId = origTe.actualId;
            te.address = origTe.address;

            table.add(te);
        }
    }

    /**
     * An implementation of closest_preceding_node in the Chord research paper.
     *
     * @param id id being searched for
     * @return closest preceding pointer
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if {@code id} has a different bit count
     * count than base pointer's id
     */
    public Pointer<A> findClosestPreceding(Id id) {
        Validate.notNull(id);
        IdUtils.ensureLimitsMatch(id, basePtr.getId());

        Id selfId = basePtr.getId();
        A selfAddress = basePtr.getAddress();

        InternalEntry foundEntry = null;
        ListIterator<InternalEntry> lit = table.listIterator(table.size());
        while (lit.hasPrevious()) {
            InternalEntry ie = lit.previous();
            // if finger[i] exists between n (exclusive) and id (exclusive)
            // then return it
            if (ie.actualId.isWithin(selfId, false, id, false)) {
                foundEntry = ie;
                break;
            }
        }

        Pointer<A> ret;
        if (foundEntry == null) {
            // nothing found, return self
            ret = new Pointer<>(selfId, selfAddress);
        } else {
            ret = new Pointer<>(foundEntry.actualId, foundEntry.address);
        }

        return ret;
    }

    /**
     * Puts a pointer in to the finger table. See the constraints / guarantees
     * mentioned in the class Javadoc: {@link FingerTable}.
     * <p/>
     * This method automatically determines the correct position for the finger.
     * The old pointer in that finger will be replaced by {@code ptr}.
     *
     * @param ptr pointer to put in as finger
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if {@code ptr}'s id has a different bit
     * count than the base pointer's id
     * @throws IllegalArgumentException if {@code ptr} has an id that matches
     * the base pointer.
     */
    public void put(Pointer<A> ptr) {
        Validate.notNull(ptr);
        IdUtils.ensureLimitsMatch(ptr, basePtr);

        Id id = ptr.getId();
        A address = ptr.getAddress();

        if (basePtr.equalsEnsureAddress(ptr)) {
            throw new IllegalArgumentException();
        }

        Id baseId = basePtr.getId();

        // search for position to insert to
        int replacePos = -1;
        for (int i = 0; i < table.size(); i++) {
            InternalEntry ie = table.get(i);
            Id goalId = ie.expectedId;
            int compVal = goalId.comparePosition(baseId, id);
            if (compVal < 0) {
                replacePos = i;
            } else if (compVal == 0) {
                replacePos = i;
                break;
            }
        }

        if (replacePos == -1) {
            return;
        }

        // replace in table
        InternalEntry entry = table.get(replacePos);
        entry.actualId = id;
        entry.address = address;


        // replace immediate preceding neighbours if they exceed replacement
        for (int i = replacePos - 1; i >= 0; i--) {
            InternalEntry priorEntry = table.get(i);

            if (priorEntry.actualId.comparePosition(baseId, id) > 0
                    || priorEntry.actualId.equals(baseId)) {
                priorEntry.actualId = id;
                priorEntry.address = address;
            } else {
                break;
            }
        }
    }

    /**
     * Similar to {@link #put(com.offbynull.peernetic.chord.BitLimitedPointer)
     * }, but makes sure that {@code ptr} is less than or equal to the expected
     * id before putting it in.
     * <p/>
     * For example, imagine a finger table for a base pointer with an id of 0
     * and a bit count of 3. This is what the initial table would look like...
     * <pre>
     * Index 0 = id:0 (base)
     * Index 1 = id:0 (base)
     * Index 2 = id:0 (base)
     * </pre> If a value pointer with id of 6 were put in here, then a pointer
     * with id of 1 were put in here, the table would look like this...
     * <pre>
     * Index 0 = id:1 (base)
     * Index 1 = id:6 (base)
     * Index 2 = id:6 (base)
     * </pre> If this method were called with a pointer that had id of 7,
     * nothing would happen. If this method were called with a pointer that had
     * id of 5, then the table would be adjusted to look like this...
     * <pre>
     * Index 0 = id:1 (base)
     * Index 1 = id:5 (base)
     * Index 2 = id:5 (base)
     * </pre>
     *
     * @param ptr pointer to add in as finger
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if {@code ptr}'s id has a different bit
     * count than the base pointer's id
     * @throws IllegalArgumentException if {@code ptr} has an id that matches
     * the base pointer.
     */
    public void replace(Pointer<A> ptr) {
        Validate.notNull(ptr);
        IdUtils.ensureLimitsMatch(ptr, basePtr);

        Id id = ptr.getId();
        A address = ptr.getAddress();

        if (basePtr.equalsEnsureAddress(ptr)) {
            throw new IllegalArgumentException();
        }

        Id baseId = basePtr.getId();

        // search for position to insert to
        int replacePos = -1;
        for (int i = 0; i < table.size(); i++) {
            InternalEntry ie = table.get(i);
            Id goalId = ie.expectedId;
            int compVal = goalId.comparePosition(baseId, id);
            if (compVal < 0) {
                replacePos = i;
            } else if (compVal == 0) {
                replacePos = i;
                break;
            }
        }

        if (replacePos == -1) {
            return;
        }


        // check if can be replaced -- if so, replace.
        InternalEntry entry = table.get(replacePos);

        Id oldId;
        if (id.comparePosition(baseId, entry.actualId) < 0
                || entry.actualId.equals(baseId)) {
            oldId = entry.actualId;
            entry.actualId = id;
            entry.address = address;

            // replace immediate preceding neighbours if they = old value
            for (int i = replacePos - 1; i >= 0; i--) {
                InternalEntry priorEntry = table.get(i);

                if (priorEntry.actualId.equals(oldId)) {
                    priorEntry.actualId = id;
                    priorEntry.address = address;
                } else {
                    break;
                }
            }
        }
    }

    /**
     * Get the base pointer.
     *
     * @return base pointer
     */
    public Pointer<A> getBase() {
        return basePtr;
    }

    /**
     * Get the id from the base pointer. Equivalent to calling
     * {@code getBase().getId()}.
     *
     * @return base pointer id
     */
    public Id getBaseId() {
        return basePtr.getId();
    }

    /**
     * Get the address from the base pointer. Equivalent to calling
     * {@code getBase().getAddress()}.
     *
     * @return base pointer address
     */
    public A getBaseAddress() {
        return basePtr.getAddress();
    }

    /**
     * Get the last/maximum finger that isn't set to base. If no such finger is
     * found, gives back {@code null}. See the constraints/guarantees mentioned
     * in the Javadoc header {@link FingerTable}.
     *
     * @return last/max non-base finger, or {@code null} if no such finger
     * exists
     */
    public Pointer<A> getMaximumNonBase() {
        Id baseId = basePtr.getId();

        for (int i = bitCount - 1; i >= 0; i--) {
            InternalEntry ie = table.get(i);
            if (!ie.actualId.equals(baseId)) {
                return new Pointer<>(ie.actualId, ie.address);
            }
        }

        return null;
    }

    /**
     * Get finger[0] if it doesn't match the base id. If it does match the base
     * id, gives back {@code null}
     *
     * @return finger[0], or {@code null} if finger[0] is base
     */
    public Pointer<A> getMinimumNonBase() {
        Id baseId = basePtr.getId();
        InternalEntry ie = table.get(0);

        Pointer<A> ret = null;

        if (!ie.actualId.equals(baseId)) {
            ret = new Pointer<>(ie.actualId, ie.address);
        }

        return ret;
    }

    /**
     * Get the finger at a specific index.
     *
     * @param idx finger index
     * @return finger at index {@code idx}
     * @throws IllegalArgumentException if {@code idx < 0 || idx > table.size()}
     */
    public Pointer<A> get(int idx) {
        Validate.inclusiveBetween(0, table.size() - 1, idx);

        InternalEntry ie = table.get(idx);
        return new Pointer<>(ie.actualId, ie.address);
    }

    /**
     * Get the id expected for a finger position. For example, if base id is 0,
     * finger pos 0 expects 1, finger pos 1 expects 2, finger pos 2 expects 4,
     * finger pos 3 expects 8, etc... For more information, see Chord research
     * paper.
     *
     * @param idx finger position to get expected id for
     * @return expected id for a specific finger position
     * @throws IllegalArgumentException if {@code idx < 0 || idx > table.size()}
     */
    public Id getExpectedId(int idx) {
        Validate.inclusiveBetween(0, table.size() - 1, idx);

        InternalEntry ie = table.get(idx);
        return ie.expectedId;
    }

    /**
     * Removes a pointer in to the finger table. If the pointer doesn't exist in
     * the finger table, does nothing. See the constraints / guarantees
     * mentioned in the class Javadoc: {@link FingerTable}.
     *
     * @param ptr pointer to put in as finger
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if {@code ptr}'s id has a different bit
     * count than the base pointer's id
     * @throws IllegalArgumentException if {@code ptr} has an id that matches
     * the base pointer.
     */
    public void remove(Pointer<A> ptr) {
        Validate.notNull(ptr);
        IdUtils.ensureLimitsMatch(ptr, basePtr);

        Id id = ptr.getId();
        A address = ptr.getAddress();

        if (basePtr.equalsEnsureAddress(ptr)) {
            throw new IllegalArgumentException();
        }

        ListIterator<InternalEntry> lit = table.listIterator(table.size());
        while (lit.hasPrevious()) {
            InternalEntry ie = lit.previous();
            Id testId = ie.actualId;
            A testAddress = ie.address;

            if (id.equals(testId) && address.equals(testAddress)) {
                remove(lit.previousIndex() + 1);
                break;
            }
        }
    }

    /**
     * Removes the pointer at index {@code idx} of the finger table. See the
     * constraints / guarantees mentioned in the class Javadoc:
     * {@link FingerTable}.
     *
     * @param idx finger position to clear
     * @throws IllegalArgumentException if {@code ptr}'s id has a different bit
     * count than the base pointer's id
     * @throws IllegalArgumentException if {@code ptr} has an id that matches
     * the base pointer.
     */
    private void remove(int idx) {
        Validate.inclusiveBetween(0, bitCount - 1, idx);

        Id baseId = basePtr.getId();
        A baseAddress = basePtr.getAddress();

        // save existing id
        InternalEntry entry = table.get(idx);
        Id oldId = entry.actualId;

        if (oldId.equals(baseId)) {
            // nothing to remove if self... all forward entries sohuld
            // also be self in this case
            return;
        }

        // get next id in table, if available
        Id nextId;
        A nextAddress;

        if (idx < bitCount - 1) {
            // set values if there's a next available (if we aren't at ceiling)
            InternalEntry nextEntry = table.get(idx + 1);
            nextId = nextEntry.actualId;
            nextAddress = nextEntry.address;
        } else {
            // set values to self if we are at the ceiling (full circle)
            nextId = baseId;
            nextAddress = baseAddress;
        }

        // replace prior ids with next id if prior same as old id and is
        // contiguous... prior ids will never be greater than the old id
        for (int i = idx; i >= 0; i--) {
            InternalEntry priorEntry = table.get(i);

            if (priorEntry.actualId.equals(oldId)) {
                priorEntry.actualId = nextId;
                priorEntry.address = nextAddress;
            } else {
                break;
            }
        }
    }

    /**
     * Removes all fingers before {@code id} (does not remove {@code id}
     * itself).
     *
     * @param id id of which all fingers before it will be removed
     * @return number of fingers that were cleared
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if {@code id} has a different bit count
     * count than base pointer's id
     */
    public int clearBefore(Id id) {
        Validate.notNull(id);
        IdUtils.ensureLimitsMatch(basePtr.getId(), id);

        Id baseId = basePtr.getId();

        if (id.equals(baseId)) {
            throw new IllegalArgumentException();
        }

        ListIterator<InternalEntry> lit = table.listIterator(table.size());
        while (lit.hasPrevious()) {
            InternalEntry ie = lit.previous();
            Id testId = ie.actualId;

            if (id.comparePosition(baseId, testId) > 0) {
                int position = lit.previousIndex() + 1;
                clearBefore(position);
                return position;
            }
        }

        return 0;
    }

    /**
     * Removes all fingers before position {@code idx} (does not remove finger
     * at {@code idx}).
     *
     * @param idx position which all fingers before it will be removed
     * @throws IllegalArgumentException if {@code idx < 0 || idx > table.size()}
     */
    private void clearBefore(int idx) {
        Validate.inclusiveBetween(0, bitCount - 1, idx);

        Id baseId = basePtr.getId();
        A baseAddress = basePtr.getAddress();

        // get next id in table, if available
        Id nextId;
        A nextAddress;

        if (idx < bitCount - 1) {
            // set values if there's a next available (if we aren't at ceiling)
            InternalEntry nextEntry = table.get(idx + 1);
            nextId = nextEntry.actualId;
            nextAddress = nextEntry.address;
        } else {
            // set values to self if we are at the ceiling (full circle)
            nextId = baseId;
            nextAddress = baseAddress;
        }

        // replace prior ids with next id... prior ids will never be greater
        // than the id at the position we're removing
        for (int i = idx; i >= 0; i--) {
            InternalEntry priorEntry = table.get(i);

            priorEntry.actualId = nextId;
            priorEntry.address = nextAddress;
        }
    }

    /**
     * Removes all fingers after {@code id} (does not remove {@code id} itself).
     *
     * @param id id of which all fingers after it will be removed
     * @return number of fingers that were cleared
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if {@code id} has a different bit count
     * count than base pointer's id
     */
    public int clearAfter(Id id) {
        Validate.notNull(id);
        IdUtils.ensureLimitsMatch(id, basePtr.getId());

        Id baseId = basePtr.getId();

        ListIterator<InternalEntry> lit = table.listIterator();
        while (lit.hasNext()) {
            InternalEntry ie = lit.next();
            Id testId = ie.actualId;

            if (id.comparePosition(baseId, testId) < 0) {
                int position = lit.nextIndex() - 1;
                clearAfter(position);
                return bitCount - position;
            }
        }

        return 0;
    }

    /**
     * Removes all fingers after position {@code idx} (does not remove finger at
     * {@code idx}).
     *
     * @param idx position which all fingers after it will be removed
     * @throws IllegalArgumentException if {@code idx < 0 || idx > table.size()}
     */
    private void clearAfter(int idx) {
        Validate.inclusiveBetween(0, bitCount - 1, idx);

        Id baseId = basePtr.getId();
        A baseAddress = basePtr.getAddress();

        // replace entries with self id all the way till the end...
        for (int i = idx; i < bitCount; i++) {
            InternalEntry priorEntry = table.get(i);

            priorEntry.actualId = baseId;
            priorEntry.address = baseAddress;
        }
    }

    /**
     * Clears the finger table. All fingers will be set to the base pointer.
     */
    public void clear() {
        Id baseId = basePtr.getId();
        A baseAddress = basePtr.getAddress();

        // replace entries with self id all the way till the end...
        for (int i = 0; i < bitCount; i++) {
            InternalEntry priorEntry = table.get(i);

            priorEntry.actualId = baseId;
            priorEntry.address = baseAddress;
        }
    }

    /**
     * Checks to see if all entries in the finger table are set to base pointer.
     *
     * @return {@code true} if all entries are set to base pointer,
     * {@code false} otherwise
     */
    public boolean isPointingToBase() {
        if (table.isEmpty()) {
            return false;
        }

        InternalEntry ie = table.get(0);
        Pointer<A> ptr = new Pointer<>(ie.actualId, ie.address);
        return ptr.equals(basePtr);
    }

    /**
     * Dumps the fingers.
     *
     * @return list of fingers
     */
    public List<Pointer<A>> dump() {
        List<Pointer<A>> ret = new ArrayList<>(table.size());

        for (InternalEntry ie : table) {
            Pointer<A> ptr = new Pointer<A>(ie.actualId, ie.address);
            ret.add(ptr);
        }

        return ret;
    }

    /**
     * Internal class to keep track of a finger.
     */
    private final class InternalEntry {

        /**
         * Desired id for finger.
         */
        private Id expectedId;
        /**
         * BitLimitedId of finger.
         */
        private Id actualId;
        /**
         * A of finger.
         */
        private A address;
    }
}
