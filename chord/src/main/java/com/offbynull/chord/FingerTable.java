package com.offbynull.chord;

import com.offbynull.chord.RouteResult.ResultType;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public final class FingerTable {
    private List<InternalEntry> table;
    private Pointer basePtr;
    private int bitCount;
    
    public FingerTable(Pointer base) {
        if (base == null) {
            throw new NullPointerException();
        }
        
        this.basePtr = base;
        
        Id baseId = base.getId();
        Address baseAddress = base.getAddress();
        
        this.bitCount = baseId.getBitCount();

        table = new ArrayList<>(bitCount);
        for (int i = 0; i < bitCount; i++) {
            BigInteger data = BigInteger.ONE.shiftLeft(i);
            byte[] offsetIdRaw = data.toByteArray();
            Id offsetId = new Id(bitCount, offsetIdRaw);
            Id expectedId = baseId.add(offsetId);
            
            InternalEntry te = new InternalEntry();
            te.expectedId = expectedId;
            te.actualId = baseId;
            te.address = baseAddress;
            
            table.add(te);
        }
    }
    
    public RouteResult route(Id id) {
        if (id == null) {
            throw new NullPointerException();
        }
        if (id.getBitCount() != bitCount) {
            throw new IllegalArgumentException();
        }
        
        InternalEntry firstEntry = table.get(0); // aka successor
        
        Pointer pointer;
        ResultType resultType;
        
        Id selfId = basePtr.getId();
        
        if (id.isWithin(selfId, false, firstEntry.actualId, true)) {
            // if id is between baseId (exclusive) and finger[0] (inclusive)
            pointer = new Pointer(firstEntry.actualId, firstEntry.address);
            resultType = ResultType.FOUND;
        } else {
            pointer = findClosestPreceding(id);
            
            if (pointer.getId().equals(selfId)) {
                resultType = ResultType.SELF;
            } else {
                resultType = ResultType.CLOSEST_PREDECESSOR;
            }
        }
        
        return new RouteResult(resultType, pointer);
    }
    
    private Pointer findClosestPreceding(Id id) {
        if (id == null) {
            throw new NullPointerException();
        }
        if (id.getBitCount() != bitCount) {
            throw new IllegalArgumentException();
        }
        
        Id selfId = basePtr.getId();
        Address selfAddress = basePtr.getAddress();
        
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
        
        Pointer ret;
        if (foundEntry == null) {
            // nothing found, return self
            ret = new Pointer(selfId, selfAddress);
        } else {
            ret = new Pointer(foundEntry.actualId, foundEntry.address);
        }
        
        return ret;
    }
    
    public void put(Pointer ptr) {
        if (ptr == null) {
            throw new NullPointerException();
        }
        
        Id id = ptr.getId();
        Address address = ptr.getAddress();
        
        Id selfId = basePtr.getId();
        Address selfAddress = basePtr.getAddress();
        
        if (id.getBitCount() != bitCount || id.equals(selfId)) {
            throw new IllegalArgumentException();
        }
        
        // search for position to insert to
        int replacePos = -1;
        for (int i = 0; i < table.size(); i++) {
            InternalEntry ie = table.get(i);
            Id goalId = ie.expectedId;
            int compVal = goalId.comparePosition(selfId, id);
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
            
            if (priorEntry.actualId.comparePosition(selfId, id) > 0
                    || priorEntry.actualId.equals(selfId)) {
                priorEntry.actualId = id;
                priorEntry.address = address;
            } else {
                break;
            }
        }
    }

    public Pointer getBase() {
        return basePtr;
    }
    
    public Id getBaseId() {
        return basePtr.getId();
    }
    
    public Address getBaseAddress() {
        return basePtr.getAddress();
    }
    
    public Pointer get(int idx) {
        if (idx < 0 || idx >= table.size()) {
            throw new IllegalArgumentException();
        }
        
        InternalEntry ie = table.get(idx);
        return new Pointer(ie.actualId, ie.address);
    }
    
    public Id getExpectedId(int idx) {
        if (idx < 0 || idx >= table.size()) {
            throw new IllegalArgumentException();
        }
        
        InternalEntry ie = table.get(idx);
        return ie.expectedId;
    }
    
    public void remove(Pointer ptr) {
        if (ptr == null) {
            throw new NullPointerException();
        }
        
        Id id = ptr.getId();
        Address address = ptr.getAddress();
        
        if (id.getBitCount() != bitCount) {
            throw new IllegalArgumentException();
        }
        
        ListIterator<InternalEntry> lit = table.listIterator(table.size());
        while (lit.hasPrevious()) {
            InternalEntry ie = lit.previous();
            Id testId = ie.actualId;
            Address testAddress = ie.address;
            
            if (id.equals(testId) && address.equals(testAddress)) {
                remove(lit.previousIndex() + 1);
                break;
            }
        }
    }
    
    private void remove(int position) {
        if (position < 0 || position >= bitCount) {
            throw new IllegalArgumentException();
        }
        
        Id baseId = basePtr.getId();
        Address baseAddress = basePtr.getAddress();
        
        // save existing id
        InternalEntry entry = table.get(position);
        Id oldId = entry.actualId;
        
        if (oldId.equals(baseId)) {
            // nothing to remove if self... all forward entries sohuld
            // also be self in this case
            return;
        }
        
        // get next id in table, if available
        Id nextId;
        Address nextAddress;

        if (position < bitCount - 1) {
            // set values if there's a next available (if we aren't at ceiling)
            InternalEntry nextEntry = table.get(position + 1);
            nextId = nextEntry.actualId;
            nextAddress = nextEntry.address;
        } else {
            // set values to self if we are at the ceiling (full circle)
            nextId = baseId;
            nextAddress = baseAddress;
        }
        
        // replace prior ids with next id if prior same as old id and is
        // contiguous... prior ids will never be greater than the old id
        for (int i = position; i >= 0; i--) {
            InternalEntry priorEntry = table.get(i);
            
            if (priorEntry.actualId.equals(oldId)) {
                priorEntry.actualId = nextId;
                priorEntry.address = nextAddress;
            } else {
                break;
            }
        }
    }

    public void clearBefore(Id id) {
        if (id == null) {
            throw new NullPointerException();
        }
        
        if (id.getBitCount() != bitCount) {
            throw new IllegalArgumentException();
        }
        
        Id baseId = basePtr.getId();
        
        ListIterator<InternalEntry> lit = table.listIterator(table.size());
        while (lit.hasPrevious()) {
            InternalEntry ie = lit.previous();
            Id testId = ie.actualId;
            
            if (id.comparePosition(baseId, testId) > 0) {
                clearBefore(lit.previousIndex() + 1);
                break;
            }
        }
    }
    
    private void clearBefore(int position) {
        if (position < 0 || position >= bitCount) {
            throw new IllegalArgumentException();
        }
        
        Id baseId = basePtr.getId();
        Address baseAddress = basePtr.getAddress();
        
        // get next id in table, if available
        Id nextId;
        Address nextAddress;

        if (position < bitCount - 1) {
            // set values if there's a next available (if we aren't at ceiling)
            InternalEntry nextEntry = table.get(position + 1);
            nextId = nextEntry.actualId;
            nextAddress = nextEntry.address;
        } else {
            // set values to self if we are at the ceiling (full circle)
            nextId = baseId;
            nextAddress = baseAddress;
        }
        
        // replace prior ids with next id... prior ids will never be greater
        // than the id at the position we're removing
        for (int i = position; i >= 0; i--) {
            InternalEntry priorEntry = table.get(i);
            
            priorEntry.actualId = nextId;
            priorEntry.address = nextAddress;
        }
    }

    public void clearAfter(Id id) {
        if (id == null) {
            throw new NullPointerException();
        }
        
        if (id.getBitCount() != bitCount) {
            throw new IllegalArgumentException();
        }
        
        Id baseId = basePtr.getId();
        
        ListIterator<InternalEntry> lit = table.listIterator();
        while (lit.hasNext()) {
            InternalEntry ie = lit.next();
            Id testId = ie.actualId;
            
            if (id.comparePosition(baseId, testId) < 0) {
                clearAfter(lit.nextIndex() - 1);
                break;
            }
        }
    }

    private void clearAfter(int position) {
        if (position < 0 || position >= bitCount) {
            throw new IllegalArgumentException();
        }
        
        Id baseId = basePtr.getId();
        Address baseAddress = basePtr.getAddress();
        
        // replace entries with self id all the way till the end...
        for (int i = position; i < bitCount; i++) {
            InternalEntry priorEntry = table.get(i);
            
            priorEntry.actualId = baseId;
            priorEntry.address = baseAddress;
        }
    }

    public List<Pointer> dump() {
        List<Pointer> ret = new ArrayList<>(table.size());
        
        for (InternalEntry ie : table) {
            Pointer ptr = new Pointer(ie.actualId, ie.address);
            ret.add(ptr);
        }
        
        return ret;
    }

    private static final class InternalEntry {
        public Id expectedId;
        public Id actualId;
        public Address address;
    }
}
