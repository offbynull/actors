package com.offbynull.peernetic.chord;

import java.util.List;

/**
 * Holds the state information for a Chord entity. State information includes
 * a successor table, a finger table, and a predecessor.
 * <p/>
 * This class attempts to keep the successor table, finger table, and
 * predecessor in sync with each other. The finger table and successor table
 * aren't allowed to exceed the predecessor. Changes to the finger table's first
 * index are propagated to the successor table (and vice versa).
 * @author Kasra Faghihi
 */
public final class ChordState {
    private Pointer basePtr;
    private FingerTable fingerTable;
    private SuccessorTable successorTable;
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
     * <b>Case 1:</b> In a 8 node ring, node 0 has it's predecessor set to
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

    public Pointer getSuccessor() {
        return successorTable.getSuccessor();
    }
    
    public void shiftSuccessor() {
        successorTable.moveToNextSucessor();

        adjustFingerTableToMatchSuccessorTable();
        adjustFingerTableToMatchPredecessor(); // finger table has changed, make
                                               // sure it doesn't exceed pred
    }

    public void setSuccessor(Pointer successor, List<Pointer> table) {
        if (successor == null || table == null || table.contains(null)) {
            throw new NullPointerException();
        }
        
        successorTable.update(successor, table);

        adjustFingerTableToMatchSuccessorTable();
        adjustFingerTableToMatchPredecessor(); // finger table has changed, make
                                               // sure it doesn't exceed pred
    }
    
    public RouteResult route(Id id) {
        if (id == null) {
            throw new NullPointerException();
        }
        
        return fingerTable.route(id);
    }
    
    public Id getExpectedFingerId(int bitPosition) {
        if (bitPosition < 0) {
            throw new IllegalArgumentException();
        }
        
        return fingerTable.getExpectedId(bitPosition);
    }

    public Pointer getFinger(int bitPosition) {
        if (bitPosition < 0) {
            throw new IllegalArgumentException();
        }
        
        return fingerTable.get(bitPosition);
    }
    
    public void putFinger(Pointer pointer) {
        if (pointer == null) {
            throw new NullPointerException();
        }
        
        fingerTable.put(pointer);
        adjustFingerTableToMatchPredecessor(); //incase pred is now < last finger
        adjustSuccessorTableToMatchFingerTable();
    }

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
    
    private void adjustSuccessorTableToMatchFingerTable() {
        // make sure successor table's value is finger table's idx 0
        Pointer successorPtr = fingerTable.get(0);
        successorTable.updateTrim(successorPtr);
    }
    
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
