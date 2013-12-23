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
package com.offbynull.peernetic.overlay.chord;

import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.IdUtils;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * Holds the state information for a Chord entity. State information includes
 * a successor table, a finger table, an update tracker, and a predecessor.
 * <p/>
 * This class attempts to keep the successor table, finger table, and
 * predecessor in sync with each other. The finger table and successor table
 * aren't allowed to exceed the predecessor. Changes to the finger table's 0
 * index are propagated to the successor table (and vice versa).
 * <p/>
 * In addition to that, this class attempts to keep track of when updates are
 * supposed to happen. Updates include fix finger updates as well as stabilizing
 * the successor.
 * @param <A> address type
 * @author Kasra Faghihi
 */
public final class ChordState<A> {
    /**
     * The pointer to this node.
     */
    private Pointer<A> basePtr;
    
    /**
     * Finger table -- accelerates key lookup. As per the Chord research paper.
     */
    private FingerTable<A> fingerTable;
    
    /**
     * Successor table -- keeps track of a recursive list of successors. As per
     * the Chord research paper.
     */
    private SuccessorTable<A> successorTable;
    
    /**
     * Pointer to this node's predecessor.
     */
    private Pointer<A> predecessorPtr;
    
    /**
     * Construct a {@link ChordState} object.
     * @param basePtr pointer to self (also known as base pointer
     * @throws NullPointerException if any of the arguments are {@code null}
     */
    public ChordState(Pointer<A> basePtr) {
        Validate.notNull(basePtr);
        
        this.basePtr = basePtr;
        fingerTable = new FingerTable(basePtr);
        successorTable = new SuccessorTable(basePtr);
        predecessorPtr = null;
    }
    
    /**
     * Get the bit length of IDs that this Chord entity is suppose to use. See
     * {@link BitLimitedId#getBitCount() }. This value is set by the base pointer passed
     * in to the constructor (see
     * {@link #ChordState(com.offbynull.peernetic.chord.BitLimitedPointer) }).
     * @return expected bit length of ids
     */
    public int getBitCount() {
        return IdUtils.getLimitBitLength(basePtr);
    }
    
    /**
     * Get the base pointer.
     * @return base pointer
     */
    public Pointer<A> getBase() {
        return basePtr;
    }
    
    /**
     * Get the ID from the base pointer.
     * @return ID for base pointer
     */
    public Id getBaseId() {
        return basePtr.getId();
    }
    
    /**
     * Get the address from the base pointer.
     * @return address for base pointer
     */
    public A getBaseAddress() {
        return basePtr.getAddress();
    }

    /**
     * Get the predecessor.
     * @return predecessor
     */
    public Pointer<A> getPredecessor() {
        return predecessorPtr;
    }

    /**
     * Set the predecessor. If an existing predecessor is set, the new
     * predecessor must be between the old predecessor (exclusive) and the
     * base (exclusive). Synchronization effects of this method are as follows:
     * <ul>
     * <li>
     * Once finger table has been modified, it needs to be checked to make sure
     * that the predecessor is at least larger than the max non-base finger
     * entry. If it isn't, then set it to the max non-base finger entry, because
     * it doesn't make sense to have a finger table entry larger than our
     * predecessor.
     * </li>
     * <li>
     * Since there's a possibility that the finger table may have been truncated
     * (see above paragraph), there's a possibility that finger[0] may no longer
     * be there. It's also possible that {@code pointer} may overwrite
     * finger[0]. We need to adjust the successor in successor table to match
     * the potentially new (or non-existent) finger[0].
     * </li>
     * </ul>
     * <b>Case 1:</b> In a 8 node ring, node 0 has it's predecessor set to
     * node 6. Node 5 asks node 0 to set it's predecessor to it. In this case,
     * node 5 isn't between the existing predecessor (node 6) and node being
     * updated (node 0), so nothing will be changed.
     * <p/>
     * <pre>
     *   0-1
     *  /   \
     * x     2
     * |     |
     * 6     3
     *  \   /
     *   5-4
     * </pre>
     * <p/>
     * <b>Case 2:</b> In a 8 node ring, node 0 has no predecessor set. Node 5
     * asks node 0 to set it's predecessor to it. In this case, node 0 clears
     * its finger table of all entries greater than or equal to node 5, then
     * sets node 5 as its predecessor.
     * <p/>
     * <pre>
     *   0-1
     *  /   \
     * x     2
     * |     |
     * x     3
     *  \   /
     *   5-4
     * </pre>
     * <p/>
     * <b>Case 3:</b> In a 8 node ring, node 0 has it's predecessor set to
     * node 5. Node 6 asks node 0 to set it's predecessor to it. In this case,
     * node 6 is between the existing predecessor (node 5) and node being
     * updated (node 0), so the predecessor will be changed. The finger table
     * will also be adjusted such that node 6 will be added in if it makes
     * sense to do this.
     * <p/>
     * <pre>
     *   0-1
     *  /   \
     * x     2
     * |     |
     * x     3
     *  \   /
     *   5-4
     * </pre>
     * <p/>
     * @throws NullPointerException if any of the arguments are {@code null}
     * @throws IllegalArgumentException if {@code predecessor} is the base
     * pointer
     * @throws IllegalArgumentException if {@code predecessor} is not between
     * the existing predecessor pointer and base pointer
     * @param predecessor new predecessor value
     */
    public void setPredecessor(Pointer<A> predecessor) {
        Validate.notNull(predecessor);
        
        if (basePtr.equalsEnsureAddress(predecessor)) {
            throw new IllegalArgumentException();
        }
        
        Id id = basePtr.getId();
        
        if (this.predecessorPtr != null) {
            Id oldId = this.predecessorPtr.getId();
            Id newId = predecessor.getId();

            if (!newId.isWithin(oldId, true, id, false)) {
                throw new IllegalArgumentException();
            }
        }
        
        this.predecessorPtr = predecessor;
        
        // make sure finger table doesn't exceed predecessor
        adjustFingerTableToMatchPredecessor();
        
        // since finger table was changed, there's a change that the index 0 was
        // changed, so we need to synch up the successor table with the finger
        // table
        adjustSuccessorTableToMatchFingerTable();
    }
    
    /**
     * Removes the predecessor. Synchronization effects of this method are as
     * follows:
     * <ul>
     * <li>
     * If the predecessor exists in the finger table, it's removed as well.
     * The predecessor is guaranteed to be at least the max non-base finger
     * entry, but may be higher (in which case it doesn't exist in the finger
     * table, so nothing gets removed).
     * </li>
     * <li>
     * Since there's a possibility that the finger table had something removed
     * (see above paragraph), there's a possibility that the removal was
     * finger[0]. We need to adjust the successor in successor table to match
     * the potentially new (or non-existent) finger[0].
     * </li>
     * </ul>
     */
    public void removePredecessor() {
        if (predecessorPtr != null) {
            // Since we're removing our predecessor, make sure it doesn't exist
            // in the finger table as well. Since predecessor will only ever
            // map to the last non-base entry in the finger table, it's safe to
            // do a remove here (instead of clearAfter and remove).
            fingerTable.remove(predecessorPtr);
            
            // since finger table was changed, there's a change that the index 0 was
            // changed, so we need to synch up the successor table with the finger
            // table
            adjustSuccessorTableToMatchFingerTable();
        }
        predecessorPtr = null;
    } 

    /**
     * Gets the successor. The successor comes from the successor table and it's
     * equivalent to calling {@link SuccessorTable#getSuccessor() }. Finger[0]
     * (index 0 of the finger table) is always in sync with the successor.
     * @return successor
     */
    public Pointer<A> getSuccessor() {
        return successorTable.getSuccessor();
    }
    
    /**
     * Shifts the successor to the next successor in the successor table.
     * Synchronization effects of this method are as follows:
     * <ul>
     * <li>
     * The new successor is different from finger[0]. As such, we need to adjust
     * the finger table so that finger[0] matches the new successor.
     * </li>
     * <li>
     * If the predecessor is unset, the max non-base value from the finger table
     * will be used as its value (unchanged if no max non-base value). This is
     * because we know that there are nodes in the system, so we have to have a
     * predecessor. The predecessor has to at least be the last max non-base
     * entry. If there's a closer predecessor, it'll notify us eventually and
     * we'll set the closer value. This also helps correct nodes that think
     * they're our predecessor but are farther away.
     * </li>
     * <li>
     * Once finger table has been modified, it needs to be checked to make sure
     * that the predecessor is at least larger than the max non-base finger
     * entry. If it isn't, then set it to the max non-base finger entry, because
     * it doesn't make sense to have a finger table entry larger than our
     * predecessor.
     * </li>
     * </ul>
     * <b>Case 1:</b> In a 8 node ring, node 0 has it's predecessor set to
     * node 1 and its successor table set to [node 1, node 7]. Node 1 fails to
     * respond to node 0's requests, so node 0 attempts to shift to the next
     * successor in its successor table (node 7). Node 7 is greater than node
     * 0's predecessor (node 1), so this is not allowed. Instead, node 0 will
     * set its successor to its predecessor (it gets set back to node 1). Node 1
     * will be prepended on to node 0's successor table, bringing the successor
     * table make to its original value: [node 1, node 7].
     * <p/>
     * An outside method will presumably be called at regular intervals to check
     * if the predecessor is alive. Eventually the predecessor will be removed,
     * allowing the successor table to move to node 7. Even if it doesn't, node
     * 7 should eventually ask node 0 to set it as its predecessor, so the
     * predecessor will be updated either way.
     * <p/>
     * <pre>
     *   0-x
     *  /   \
     * 7     x
     * |     |
     * x     x
     *  \   /
     *   x-x
     * </pre>
     * <p/>
     * <b>Case 2:</b> In a 8 node ring, node 0 has it's predecessor set to
     * node 7 and its successor table set to [node 1, node 2, node 7]. Node 1
     * fails to respond to node 0's requests, so node 0 moves to the next
     * successor in its successor table (node 2). Node 2 is less than node 0's
     * predecessor, so everything should be fine. The removal of node 1 from the
     * successor table gets sync'd up with the finger table -- node 1 gets
     * removed from index 0 and node 2 takes its place. The successor table will
     * end up looking like this: [node 2, node 7].
     * <p/>
     * <pre>
     *   0-x
     *  /   \
     * 7     2
     * |     |
     * x     x
     *  \   /
     *   x-x
     * </pre>
     * @throws IllegalStateException if successor table has no other nodes to
     * shift to
     */
    public void shiftSuccessor() {
        successorTable.moveToNextSucessor();

        // successor table was updated, which means that the successor may have
        // changed -- synch up finger table to the successor table
        adjustFingerTableToMatchSuccessorTable();
        
        // if pred unset or max non-base finger is greater than pred, then set
        // pred to max non-base finger
        derivePredecessorFromFingerTable();
        
        // since finger table was changed, it means that there may be an entry
        // that exceeds the predecessor. truncate it so that it doesn't
        adjustFingerTableToMatchPredecessor();
    }

    /**
     * Resets the successor table with new successors. Synchronization effects
     * of this method are as follows:
     * <ul>
     * <li>
     * The new successor is likely different from finger[0]. As such, we need
     * to adjust the finger table so that finger[0] matches the new successor.
     * </li>
     * <li>
     * If the predecessor is unset, the max non-base value from the finger table
     * will be used as its value (unchanged if no max non-base value). This is
     * because we know that there are nodes in the system, so we have to have a
     * predecessor. The predecessor has to at least be the last max non-base
     * entry. If there's a closer predecessor, it'll notify us eventually and
     * we'll set the closer value. This also helps correct nodes that think
     * they're our predecessor but are farther away.
     * </li>
     * <li>
     * Once finger table has been modified, it needs to be checked to make sure
     * that the predecessor is at least larger than the max non-base finger
     * entry. If it isn't, then set it to the max non-base finger entry, because
     * it doesn't make sense to have a finger table entry larger than our
     * predecessor.
     * </li>
     * </ul>
     * This method is very similar to how {@link #shiftSuccessor() } operates.
     * The main difference is that we're accepting a new successor from an
     * outside source rather than shifting to the next successor in our
     * successor table. See use-cases in {@link #shiftSuccessor() } javadoc to
     * gain a better understanding of what happens when this method gets called.
     * @see #shiftSuccessor() 
     * @param successor new successor
     * @param table successor table of the new successor being passed in
     * @throws NullPointerException if any arguments are {@code null}
     * @throws NullPointerException if {@code table} contains a {@code null}
     * element
     * @throws IllegalArgumentException if any incoming pointer's bit count
     * doesn't match the base pointer's bit count
     */
    public void setSuccessor(Pointer<A> successor, List<Pointer<A>> table) {
        Validate.notNull(successor);
        Validate.noNullElements(table);
        
        successorTable.update(successor, table);

        // successor table was updated, which means that the successor may have
        // changed -- synch up finger table to the successor table
        adjustFingerTableToMatchSuccessorTable();
        
        // if pred unset or max non-base finger is greater than pred, then set
        // pred to max non-base finger
        derivePredecessorFromFingerTable();
        
        // since finger table was changed, it means that there may be an entry
        // that exceeds the predecessor. truncate it so that it doesn't
        adjustFingerTableToMatchPredecessor();
    }
    
    /**
     * Attempts to route to the given id, as specified in the original chord
     * algorithm (find_successor / closest_preceding_node). This is a single
     * step in the algorithm.
     * <p/>
     * This method just calls
     * {@link FingerTable#route(com.offbynull.peernetic.chord.BitLimitedId) }.
     * @param id id being searched for
     * @return routing results
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code id}'s bit count doesn't match
     * the base pointer's bit count
     */
    public RouteResult route(Id id) {
        Validate.notNull(id);
        
        return fingerTable.route(id);
    }
    
    /**
     * Gets the expected id for a specific finger table index. This is
     * equivalent to calling {@link FingerTable#getExpectedId(int) }.
     * @param bitPosition finger table index
     * @return id for finger table index
     * @throws IllegalArgumentException if
     * {@code bitPosition < 0 || >= bitCount}
     */
    public Id getExpectedFingerId(int bitPosition) {
        Validate.inclusiveBetween(0, getBitCount() - 1, bitPosition);
        
        return fingerTable.getExpectedId(bitPosition);
    }

    /**
     * Gets the id set for a specific finger table index. This is equivalent to
     * calling {@link FingerTable#get(int) }.
     * @param bitPosition finger table index
     * @return id for finger table index
     * @throws IllegalArgumentException if
     * {@code bitPosition < 0 || >= bitCount}
     */
    public Pointer<A> getFinger(int bitPosition) {
        Validate.inclusiveBetween(0, getBitCount() - 1, bitPosition);
        
        return fingerTable.get(bitPosition);
    }
    
    /**
     * Inserts a finger into the finger table (see
     * {@link FingerTable#put(com.offbynull.peernetic.chord.BitLimitedPointer) } for
     * insertion algorithm). Synchronization effects of this method are as
     * follows:
     * <ul>
     * <li>
     * If the predecessor is unset, the max non-base value from the finger table
     * will be used as its value (unchanged if no max non-base value). This is
     * because we know that there are nodes in the system, so we have to have a
     * predecessor. The predecessor has to at least be the last max non-base
     * entry. If there's a closer predecessor, it'll notify us eventually and
     * we'll set the closer value. This also helps correct nodes that think
     * they're our predecessor but are farther away.
     * </li>
     * <li>
     * Once finger table has been modified, it needs to be checked to make sure
     * that the predecessor is at least larger than the max non-base finger
     * entry. If it isn't, then set it to the max non-base finger entry, because
     * it doesn't make sense to have a finger table entry larger than our
     * predecessor.
     * </li>
     * <li>
     * Since there's a possibility that the finger table may have been truncated
     * (see above paragraph), there's a possibility that finger[0] may no longer
     * be there. It's also possible that {@code pointer} may overwrite
     * finger[0]. We need to adjust the successor in successor table to match
     * the potentially new (or non-existent) finger[0].
     * </li>
     * </ul>
     * <b>Case 1:</b> In a 8 node ring, node 0 has it's predecessor set to
     * node 1 and its finger table set to [node 1, node 0, node 0]. Node 0 finds
     * node 7 and attempts to put node 7 in to its finger table. Node 7 is
     * greater than node 0's predecessor (node 1), so this is not allowed. node
     * 0 will truncate all values in the finger table > node 1, bringing the
     * finger table to its original value: [node 1, node 0, node 0].
     * <p/>
     * At some point node 7 should eventually ask node 0 to set it as its
     * predecessor, so the predecessor will be updated eventually. As a part of
     * that process, the predecessor will automatically be pushed in to the
     * finger table giving the result we want. This only happens if there are
     * unset fingers that the predecessor can occupy
     * (see {@link #setPredecessor(com.offbynull.peernetic.chord.BitLimitedPointer) } for
     * more information on this.
     * <p/>
     * Even if the predecessor changes weren't propagated to the finger table,
     * once the predecessor is updated, the next time this finger tries to get
     * set, it'll be successful.
     * <p/>
     * <pre>
     *   0-1
     *  /   \
     * 7     x
     * |     |
     * x     x
     *  \   /
     *   x-x
     * </pre>
     * <b>Case 2:</b> In a 8 node ring, node 0 has it's predecessor set to
     * node 7 and its finger table set to [node 1, node 7, node 7]. Node 0 finds
     * node 3 and attempts to put node 3 in to its finger table. Node 3 is less
     * than node 0's predecessor (node 7), so this is fine. Node 0 will end up
     * with this finger table: [node 1, node 3, node 6].
     * <p/>
     * <pre>
     *   0-1
     *  /   \
     * 7     x
     * |     |
     * x     3
     *  \   /
     *   x-x
     * </pre>
     * <p/>
     * <b>Case 2:</b> In a 8 node ring, node 0 has it's predecessor set to
     * nothing and its finger table set to [node 0, node 0, node 0]. Node 0
     * finds node 3 and attempts to put node 3 in to its finger table. Node 0's
     * predecessor isn't set, so this is fine. Node 0 will end up with this
     * finger table: [node 3, node 3, node 0]. Index 0 of the finger table will
     * be copied to the successor table to keep the finger table and the
     * successor table in sync. Since node 0's predecessor is set to nothing,
     * it'll now be set to the max non-base finger entry in the new finger
     * table (node 3).
     * <p/>
     * <pre>
     *   0-x
     *  /   \
     * x     x
     * |     |
     * x     3
     *  \   /
     *   x-x
     * </pre>
     * @param pointer pointer to add to the finger table
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if {@code pointers}'s bit count doesn't
     * match the base pointer's bit count
     */
    public void putFinger(Pointer<A> pointer) {
        Validate.notNull(pointer);
        
        fingerTable.put(pointer);
        
        // if pred unset or max non-base finger is greater than pred, then set
        // pred to max non-base finger
        derivePredecessorFromFingerTable();
        
        // truncate finger table such that all entries are <= pred
        //   if the pred was unset before the above call, this will do nothing
        adjustFingerTableToMatchPredecessor();
        
        // since the finger may have been truncated by the above call, index 0
        // of finger table (successpr) may not be the same anymore -- sync
        // successor table to finger table
        adjustSuccessorTableToMatchFingerTable();
    }

    /**
     * Remove a finger from the finger table. Synchronization effects of this
     * method are as follows:
     * <ul>
     * <li>
     * If {@code pointer} matches the max non-base entry in the finger table,
     * and it also matches the predecessor, then the predecessor will be
     * set to the new max non-base entry once the pointer (which is the current
     * max non-base entry) is removed. We do this because we're assuming the
     * finger is dead (why else would it be getting removed?). If it matches the
     * predecessor, then the predecessor is likely also dead.
     * </li>
     * <li>
     * Since there's a possibility that {@code pointer} may match finger[0], 
     * we need to adjust the successor in successor table to match the
     * potentially new (or non-existent) finger[0].
     * </li>
     * </ul>
     * @param pointer pointer to remove
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if {@code pointers}'s bit count doesn't
     * match the base pointer's bit count
     */
    public void removeFinger(Pointer<A> pointer) {
        Validate.notNull(pointer);
        
        Pointer<A> oldMaxNonBaseFingerPtr = fingerTable.getMaximumNonBase();
        fingerTable.remove(pointer);
        
        // If finger is max non-base finger entry and also the predecessor,
        // remove it from the finger table and set predecessor to the new max
        // non-base finger entry (which may be null, which will mark the pred
        // as unset, which is fine). The removal is probably due to the finger
        // being unresponsive, so the predecessor will be unresponsive as well.
        if (pointer.equals(oldMaxNonBaseFingerPtr)
                && pointer.equals(predecessorPtr)) {
            Pointer<A> newMaxNonBaseFingerPtr = fingerTable.getMaximumNonBase();
            predecessorPtr = newMaxNonBaseFingerPtr;
        }
        
        // There's no reason to truncate the finger table to the predecessor
        // since we're only removing, and the predecessor is always >= max
        // non-base finger entry
        
        // Index 0 (aka the successor) may have been the pointer removed, make
        // sure this propogates to the successor table.
        adjustSuccessorTableToMatchFingerTable();
    }
    
    /**
     * Dump the finger table.
     * @return list of fingers in the finger table
     */
    public List<Pointer<A>> dumpFingerTable() {
        return fingerTable.dump();
    }

    /**
     * Dump the successor table.
     * @return list of successor in the finger table
     */
    public List<Pointer<A>> dumpSuccessorTable() {
        return successorTable.dump();
    }
    
    /**
     * If predecessor is unset or is less than the max non-base entry in the
     * finger table, set it to the max non-base entry in the finger table (or
     * unset it if it doesn't exist).
     */
    private void derivePredecessorFromFingerTable() {
        Pointer<A> maxNonBaseFingerPtr = fingerTable.getMaximumNonBase();
        
        if (predecessorPtr != null) {
            if (maxNonBaseFingerPtr == null) {
                // predecessor is not null, so potentially setting it to null
                // would mean we'd be removing it, which is not what we want
                return;
            }
            
            Id baseId = basePtr.getId();
            Id predecessorId = predecessorPtr.getId();
            Id maxNonBaseFingerId = maxNonBaseFingerPtr.getId();
            
            if (predecessorId.comparePosition(baseId, maxNonBaseFingerId) > 0) {
                // predecessor is greater than max non-base finger, so don't
                // do anything
                return;
            }
        }
        
        predecessorPtr = maxNonBaseFingerPtr;
    }
    
    /**
     * Modifies the finger table such that no entry is greater than the
     * predecessor. If the finger table has no entries in it (other than base),
     * the predecessor will be put inside, making at least index 0 (also known
     * as the successor) set to the predecessor.
     * <p/>
     * Since this method potentially modifies index 0, it makes sure that the
     * successor table's first entry matches up with the finger table's
     * successor.
     * <p/>
     * <b>Note</b>: This method does nothing if the predecessor is not set.
     */
    private void adjustFingerTableToMatchPredecessor() {
        if (predecessorPtr == null) {
            return;
        }
        
        Id baseId = basePtr.getId();
        
        Pointer<A> lastFingerPtr = fingerTable.getMaximumNonBase();
        
        if (lastFingerPtr != null) {
            Id lastFingerId = lastFingerPtr.getId();
            Id predecessorId = predecessorPtr.getId();

            if (predecessorId.comparePosition(baseId, lastFingerId) < 0) {
                fingerTable.clearAfter(predecessorId);
            }
        }
        
        fingerTable.replace(predecessorPtr);
        
        // ensure successor table in sync with finger table
        adjustSuccessorTableToMatchFingerTable();
    }
    
    /**
     * Synchs up the successor table with the finger table by trimming it 
     * to finger table's index 0. 
     */
    private void adjustSuccessorTableToMatchFingerTable() {
        // make sure successor table's value is finger table's idx 0
        Pointer<A> successorPtr = fingerTable.get(0);
        successorTable.updateTrim(successorPtr);
    }

    /**
     * Synchs up the finger table to match up with the current successor in
     * successor table.
     */
    private void adjustFingerTableToMatchSuccessorTable() {
        Pointer<A> successorPtr = successorTable.getSuccessor();

        if (basePtr.equalsEnsureAddress(successorPtr)) {
            // if the successor is us, clear the finger table such that all
            // fingers = base.
            fingerTable.clear();
        } else {
            // otherwise, get the successor and set it to the finger table
            Id successorId = successorPtr.getId();

                // just to make sure this goes in idx 0, clear everything before
                // the new successor and shove it in
            fingerTable.clearBefore(successorId);
            fingerTable.put(successorPtr);
        }
    }
}