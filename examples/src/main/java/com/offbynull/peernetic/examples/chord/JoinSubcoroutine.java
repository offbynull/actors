package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.chord.model.FingerTable;
import com.offbynull.peernetic.examples.chord.model.SuccessorTable;
import org.apache.commons.lang3.Validate;

final class JoinSubcoroutine implements Coroutine {

    private final Address sourceId;
    
    private final State state;
    private final Address timerAddress;
    private final Address logAddress;
    
    private final Address bootstrapAddress;

    public JoinSubcoroutine(Address sourceId, State state, Address timerAddress, Address logAddress, Address bootstrapAddress) {
        Validate.notNull(sourceId);
        Validate.notNull(state);
        Validate.notNull(timerAddress);
        Validate.notNull(logAddress);
//        Validate.notNull(bootstrapAddress); // can be null
        this.sourceId = sourceId;
        this.state = state;
        this.timerAddress = timerAddress;
        this.logAddress = logAddress;
        this.bootstrapAddress = bootstrapAddress;
    }

    @Override
    public void run(Continuation cnt) throws Exception {
        try {
            // initialize state
            FingerTable fingerTable = new FingerTable(state.getSelfPointer());
            SuccessorTable successorTable = new SuccessorTable(state.getSelfPointer());

            // if no bootstrap address, we're the originator node, so initial successortable+fingertable is what we want.
            if (bootstrapAddress == null) {
                state.setTables(fingerTable, successorTable);
                return;
            }


            funnelToInitFingerTableCoroutine(cnt, bootstrapAddress);
        } catch (RuntimeException coe) {
            // this is a critical operation. if any of the tasks/io fail, then send up the chain
            throw new IllegalStateException("Join failed.", coe);
        }
    }

    private void funnelToInitFingerTableCoroutine(Continuation cnt, Address initialAddress) throws Exception {
        Validate.notNull(cnt);
        Validate.notNull(initialAddress);
        
        String idSuffix = "" + state.nextRandomId();
        InitFingerTableSubcoroutine innerCoroutine = new InitFingerTableSubcoroutine(
                sourceId.appendSuffix(idSuffix),
                state,
                timerAddress,
                logAddress,
                initialAddress);
        innerCoroutine.run(cnt);
    }
}
