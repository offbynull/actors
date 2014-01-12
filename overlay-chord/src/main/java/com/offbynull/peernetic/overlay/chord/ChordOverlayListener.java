package com.offbynull.peernetic.overlay.chord;

import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.util.List;

public interface ChordOverlayListener<A> {
    public void stateUpdated(String event,
            Pointer<A> self, Pointer<A> predecessor, List<Pointer<A>> fingerTable, List<Pointer<A>> successorTable);
    public void failed(FailureMode failureMode);
    
    public enum FailureMode {
        SUCCESSOR_TABLE_DEPLETED
    }
}
