package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdResponse;
import com.offbynull.peernetic.examples.chord.model.FingerTable;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.chord.model.SuccessorTable;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class JoinTask implements Coroutine {
    
    private static final Logger LOG = LoggerFactory.getLogger(JoinTask.class);

    private final State state;

    public JoinTask(State state) {
        Validate.notNull(state);
        this.state = state;
    }

    @Override
    public void run(Continuation cnt) throws Exception {
        try {
            // initialize state
            String initialAddress = state.getBootstrapAddress();
            FingerTable fingerTable = new FingerTable(state.getSelfPointer());
            SuccessorTable successorTable = new SuccessorTable(state.getSelfPointer());

            // if no bootstrap address, we're the originator node, so initial successortable+fingertable is what we want.
            if (initialAddress == null) {
                state.setTables(fingerTable, successorTable);
                return;
            }

            // ask for bootstrap node's id
              // retry this req lots of time if it fails -- this is an important step, because it's required for the init finger table task to
              // work properly. it's okay if the init finger table task only resolves some fingers, but it won't be able to resolve any if we
              // don't have our bootstrap's id.
            GetIdResponse gir = chordHelper.sendGetIdRequest(initialAddress);
            NodeId initialId = state.toId(gir.getChordId());

            // init finger table, successor table, etc...
            chordHelper.runInitFingerTableTask(initialAddress, initialId);

            // notify our fingers that we're here, we don't need to wait until finished
            chordHelper.fireUpdateOthersTask();
        } catch (RuntimeException coe) {
            // this is a critical operation. if any of the tasks/io fail, then send up the chain
            throw new IllegalStateException("Join failed.", coe);
        }
    }
}
