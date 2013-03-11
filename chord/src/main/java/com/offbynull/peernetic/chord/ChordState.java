package com.offbynull.peernetic.chord;

import java.util.List;

/**
 * Holds the state information for a Chord entity. State information includes
 * a successor table, a finger table, and a predecessor.
 * <p/>
 * This class attempts to keep the successor table, finger table, and
 * predecessor in sync with each other. The finger table and successor table
 * aren't allowed to exceed the predecessor. Changes to the finger table's 0
 * index are propagated to the successor table (and vice versa).
 * @author Kasra Faghihi
 */
public final class ChordState {
    /**
     * The pointer to this node.
     */
    private Pointer basePtr;
    
    /**
     * Finger table -- accelerates key lookup. As per the Chord research paper.
     */
    private FingerTable fingerTable;
    
    /**
     * Successor table -- keeps track of a recursive list of successors. As per
     * the Chord research paper.
     */
    private SuccessorTable successorTable;
    
    /**
     * The pointer to this node's predecessor.
     */
    private Pointer predecessorPtr;

    /**
     * Construct a {@link ChordState} object.
     * @param basePtr pointer to self (also known as base pointer
     * @throws NullPointerException if any of the arguments are {@code null}
     */
    public ChordState(Pointer basePtr) {
        if (basePtr == null) {
            throw new NullPointerException();
        }
        this.basePtr = basePtr;
        fingerTable = new FingerTable(basePtr);
        successorTable = new SuccessorTable(basePtr);
        predecessorPtr = null;
    }

    /**
     * Get the bit length of IDs that this Chord entity is suppose to use.
     * <p/>
     * For example:
     * <ul>
     * <li>1 bit = max value of 1b = address space from 0 to 1</li>
     * <li>2 bits = max value of 11b = address space from 0 to 3</li>
     * <li>3 bits = max value of 111b = address space from 0 to 7<li>
     * </ul>
     * If bit count = n, the address space will be from 0 to
     * {@code Math.pow(2, n) - 1}.
     * @return 
     */
    public int getBitCount() {
        return basePtr.getId().getBitCount();
    }
    
    /**
     * Get the base pointer.
     * @return base pointer
     */
    public Pointer getBase() {
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
    public Address getBaseAddress() {
        return basePtr.getAddress();
    }

    /**
     * Get the predecessor.
     * @return predecessor
     */
    public Pointer getPredecessor() {
        return predecessorPtr;
    }

    /**
     * Set the predecessor. If an existing predecessor is set, the new
     * predecessor must be between the old predecessor (exclusive) and the
     * base (exclusive). If the new predecessor is less than pointers in the
     * finger table, the finger table will be truncated  such that no values
     * exceed the new predecessor (if index 0 of finger table is changes, that
     * change will be moved over to the successor table).
     * <p/>
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
     * @throws IllegalArgumentException if new predecessor is the base pointer
     * @throws IllegalArgumentException if new predecessor is not between
     * existing predecessor and base
     * @param predecessor new predecessor value
     */
    public void setPredecessor(Pointer predecessor) {
        if (predecessor == null) {
            throw new NullPointerException();
        }
        
        if (PointerUtils.selfPointerTest(basePtr, predecessor)) {
            throw new IllegalArgumentException();
        }
        
        Id id = basePtr.getId();
        
        if (this.predecessorPtr == null) {
            this.predecessorPtr = predecessor;
        } else {
            Id oldId = this.predecessorPtr.getId();
            Id newId = predecessor.getId();

            if (!newId.isWithin(oldId, false, id, false)) {
                throw new IllegalArgumentException();
            }
        }
        
        adjustFingerTableToMatchPredecessor();
    }
    
    /**
     * Removes the predecessor. If the predecessor exists in the finger table,
     * it's removed as well. If it does exist in the finger table, it'll be the
     * last non-base entry.
     */
    public void removePredecessor() {
        if (predecessorPtr != null) {
            // Since we're removing our predecessor, make sure it doesn't exist
            // in the finger table as well. Since predecessor will only ever
            // map to the last non-base entry in the finger table, it's safe to
            // do a remove here (instead of clearAfter and remove).
            fingerTable.remove(predecessorPtr);
        }
        predecessorPtr = null;
    } 

    /**
     * Gets the successor. The successor is also the name of index 0 of the
     * finger table. Index 0 of the finger table and index 0 of the successor
     * table are always in sync with each other.
     * @return successor
     */
    public Pointer getSuccessor() {
        return successorTable.getSuccessor();
    }
    
    /**
     * Shifts the successor to the next successor in the successor table. The
     * new successor will always be higher than the previous successor, which
     * means that it has the possibility of exceeding the predecessor. If it
     * does exceed the predecessor, then the successor will be set to the
     * predecessor. The finger table will be adjusted such that its index 0
     * matches up with the new successor. 
     * <p/>
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

        adjustFingerTableToMatchSuccessorTable();
        adjustFingerTableToMatchPredecessor(); // finger table has changed, make
                                               // sure it doesn't exceed pred
    }

    /**
     * Resets the successor table with new successors. The new successor has the
     * possibility of exceeding the predecessor. If it does exceed the
     * predecessor, then the successor will be set to the predecessor. The
     * finger table will be adjusted such that its index 0 matches up with the
     * new successor.
     * <p/>
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
    public void setSuccessor(Pointer successor, List<Pointer> table) {
        if (successor == null || table == null || table.contains(null)) {
            throw new NullPointerException();
        }
        
        successorTable.update(successor, table);

        adjustFingerTableToMatchSuccessorTable();
        adjustFingerTableToMatchPredecessor(); // finger table has changed, make
                                               // sure it doesn't exceed pred
        
        // TODO: If predecessor is unset? should be force it to be set here?
    }
    
    /**
     * Attempts to route to the given id, as specified in the original chord
     * algorithm (find_successor / closest_preceding_node). This is a single
     * step in the algorithm.
     * <p/>
     * This method just calls
     * {@link FingerTable#route(com.offbynull.peernetic.chord.Id) }.
     * @param id id being searched for
     * @return routing results
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code id}'s bit count doesn't match
     * the base pointer's bit count
     */
    public RouteResult route(Id id) {
        if (id == null) {
            throw new NullPointerException();
        }
        
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
        if (bitPosition < 0 || bitPosition >= getBitCount()) {
            throw new IllegalArgumentException();
        }
        
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
    public Pointer getFinger(int bitPosition) {
        if (bitPosition < 0 || bitPosition >= getBitCount()) {
            throw new IllegalArgumentException();
        }
        
        return fingerTable.get(bitPosition);
    }
    
    /**
     * Inserts a finger into the finger table (see
     * {@link FingerTable#put(com.offbynull.peernetic.chord.Pointer) } for
     * insertion algorithm). Once the finger has been added in, if the
     * predecessor is less than the last non-base entry, the finger table will
     * be truncated such that no values exceed the predecessor, and the
     * predecessor will be put in as the last non-base entry in the finger
     * table. Since the finger table may get truncated, there's a possibility
     * that finger[0] may get changed. As such, the finger[0] is resynch'd with
     * the successor table.
     * <p/>
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
     * (see {@link #setPredecessor(com.offbynull.peernetic.chord.Pointer) } for
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
     * successor table in sync. The predecessor will remain unset even though
     * some new values have been pushed in to the finger table (should this be
     * changed so that if the predecessor is null, it gets set to the max
     * non-base finger entry?).
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
    public void putFinger(Pointer pointer) {
        if (pointer == null) {
            throw new NullPointerException();
        }
        
        fingerTable.put(pointer);
        adjustFingerTableToMatchPredecessor(); //incase pred is now < last finger
        adjustSuccessorTableToMatchFingerTable();
        
        // TODO: If predecessor is unset? should be force it to be set here?
    }

    /**
     * Remove a finger from the finger table. If the finger is the successor,
     * the successor table will be truncated to match the new finger table. If
     * the removed finger equals the predecessor, and the removed finger was the
     * max non-base entry in the finger table, then unset the predecessor.
     * @param pointer pointer to remove
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if {@code pointers}'s bit count doesn't
     * match the base pointer's bit count
     */
    public void removeFinger(Pointer pointer) {
        if (pointer == null) {
            throw new NullPointerException();
        }
        
        Pointer maxNonBaseFingerPtr = fingerTable.getMaximumNonBase();
        fingerTable.remove(pointer);
        
        // If finger is the predecessor and fingerTable successfully removed a
        // finger, then you may want to remove predecessor as well -- pred will
        // always be >= last non-base entry in the finger table (or null), so
        // if it equals the predecessor and something was actually removed in
        // the call to fingerTable's remove method, then it's probably okay to
        // unset predecessor here.
        if (pointer.equals(maxNonBaseFingerPtr)
                && pointer.equals(predecessorPtr)) {
            removePredecessor();
        }
        
        adjustFingerTableToMatchPredecessor(); //incase pred is now < last finger
        adjustSuccessorTableToMatchFingerTable();
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
        
        Pointer lastFingerPtr = fingerTable.getMaximumNonBase();
        
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
        Pointer successorPtr = fingerTable.get(0);
        successorTable.updateTrim(successorPtr);
    }

    /**
     * Synchs up the finger table to match up with the current successor in
     * successor table.
     */
    private void adjustFingerTableToMatchSuccessorTable() {
        Pointer successorPtr = successorTable.getSuccessor();

        if (PointerUtils.selfPointerTest(basePtr, successorPtr)) {
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
