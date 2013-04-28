package com.offbynull.peernetic.p2ptools.overlay.chord;

import com.offbynull.peernetic.p2ptools.identification.BitLimitedId;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedPointer;
import com.offbynull.peernetic.p2ptools.overlay.chord.FingerTable.RouteResult;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * Holds the state information for a Chord entity. State information includes
 * a successor table, a finger table, and a predecessor.
 * @author Kasra Faghihi
 */
public class ChordState {
    /**
     * The pointer to this node.
     */
    protected BitLimitedPointer basePtr;
    
    /**
     * Finger table -- accelerates key lookup. As per the Chord research paper.
     */
    protected FingerTable fingerTable;
    
    /**
     * Successor table -- keeps track of a recursive list of successors. As per
     * the Chord research paper.
     */
    protected SuccessorTable successorTable;
    
    /**
     * The pointer to this node's predecessor.
     */
    protected BitLimitedPointer predecessorPtr;

    /**
     * Construct a {@link ChordState} object.
     * @param basePtr pointer to self (also known as base pointer
     * @throws NullPointerException if any of the arguments are {@code null}
     */
    public ChordState(BitLimitedPointer basePtr) {
        if (basePtr == null) {
            throw new NullPointerException();
        }
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
        return basePtr.getId().getBitCount();
    }
    
    /**
     * Get the base pointer.
     * @return base pointer
     */
    public BitLimitedPointer getBase() {
        return basePtr;
    }
    
    /**
     * Get the ID from the base pointer.
     * @return ID for base pointer
     */
    public BitLimitedId getBaseId() {
        return basePtr.getId();
    }
    
    /**
     * Get the address from the base pointer.
     * @return address for base pointer
     */
    public InetSocketAddress getBaseAddress() {
        return basePtr.getAddress();
    }

    /**
     * Get the predecessor.
     * @return predecessor
     */
    public BitLimitedPointer getPredecessor() {
        return predecessorPtr;
    }

    /**
     * Set the predecessor. If an existing predecessor is set, the new
     * predecessor must be between the old predecessor (exclusive) and the
     * base (exclusive).
     * @throws NullPointerException if any of the arguments are {@code null}
     * @throws IllegalArgumentException if {@code predecessor} is the base
     * pointer
     * @throws IllegalArgumentException if {@code predecessor} is not between
     * the existing predecessor pointer and base pointer
     * @param predecessor new predecessor value
     */
    public void setPredecessor(BitLimitedPointer predecessor) {
        if (predecessor == null) {
            throw new NullPointerException();
        }
        
        if (basePtr.equalsEnsureAddress(predecessor)) {
            throw new IllegalArgumentException();
        }
        
        BitLimitedId id = basePtr.getId();
        
        if (this.predecessorPtr != null) {
            BitLimitedId oldId = this.predecessorPtr.getId();
            BitLimitedId newId = predecessor.getId();

            if (!newId.isWithin(oldId, true, id, false)) {
                throw new IllegalArgumentException();
            }
        }
        
        this.predecessorPtr = predecessor;
    }
    
    /**
     * Removes the predecessor.
     */
    public void removePredecessor() {
        predecessorPtr = null;
    } 

    /**
     * Gets the successor. The successor comes from the successor table and it's
     * equivalent to calling {@link SuccessorTable#getSuccessor() }. Finger[0]
     * (index 0 of the finger table) is always in sync with the successor.
     * @return successor
     */
    public BitLimitedPointer getSuccessor() {
        return successorTable.getSuccessor();
    }
    
    /**
     * Shifts the successor to the next successor in the successor table.
     * @throws IllegalStateException if successor table has no other nodes to
     * shift to
     */
    public void shiftSuccessor() {
        successorTable.moveToNextSucessor();
        adjustFingerTableToMatchSuccessorTable();
    }

    /**
     * Resets the successor table with new successors.
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
    public void setSuccessor(BitLimitedPointer successor, List<BitLimitedPointer> table) {
        if (successor == null || table == null || table.contains(null)) {
            throw new NullPointerException();
        }
        
        successorTable.update(successor, table);

        // successor table was updated, which means that the successor may have
        // changed -- synch up finger table to the successor table
        adjustFingerTableToMatchSuccessorTable();
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
    public RouteResult route(BitLimitedId id) {
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
    public BitLimitedId getExpectedFingerId(int bitPosition) {
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
    public BitLimitedPointer getFinger(int bitPosition) {
        if (bitPosition < 0 || bitPosition >= getBitCount()) {
            throw new IllegalArgumentException();
        }
        
        return fingerTable.get(bitPosition);
    }
    
    /**
     * Inserts a finger into the finger table (see
     * {@link FingerTable#put(com.offbynull.peernetic.chord.BitLimitedPointer) }
     * for insertion algorithm).
     * @param pointer pointer to add to the finger table
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if {@code pointers}'s bit count doesn't
     * match the base pointer's bit count
     */
    public void putFinger(BitLimitedPointer pointer) {
        if (pointer == null) {
            throw new NullPointerException();
        }
        
        fingerTable.put(pointer);
        adjustSuccessorTableToMatchFingerTable();
    }

    /**
     * Remove a finger from the finger table.
     * @param pointer pointer to remove
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if {@code pointers}'s bit count doesn't
     * match the base pointer's bit count
     */
    public void removeFinger(BitLimitedPointer pointer) {
        if (pointer == null) {
            throw new NullPointerException();
        }
        
        fingerTable.remove(pointer);
        adjustSuccessorTableToMatchFingerTable();
    }
    
    /**
     * Synchs up the successor table with the finger table by trimming it 
     * to finger table's index 0. 
     */
    protected void adjustSuccessorTableToMatchFingerTable() {
        // make sure successor table's value is finger table's idx 0
        BitLimitedPointer successorPtr = fingerTable.get(0);
        successorTable.updateTrim(successorPtr);
    }

    /**
     * Synchs up the finger table to match up with the current successor in
     * successor table.
     */
    protected void adjustFingerTableToMatchSuccessorTable() {
        BitLimitedPointer successorPtr = successorTable.getSuccessor();

        if (basePtr.equalsEnsureAddress(successorPtr)) {
            // if the successor is us, clear the finger table such that all
            // fingers = base.
            fingerTable.clear();
        } else {
            // otherwise, get the successor and set it to the finger table
            BitLimitedId successorId = successorPtr.getId();

                // just to make sure this goes in idx 0, clear everything before
                // the new successor and shove it in
            fingerTable.clearBefore(successorId);
            fingerTable.put(successorPtr);
        }
    }
}
