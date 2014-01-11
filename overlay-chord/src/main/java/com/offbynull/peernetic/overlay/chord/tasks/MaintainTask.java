package com.offbynull.peernetic.overlay.chord.tasks;

import com.offbynull.peernetic.actor.helpers.AbstractMultiTask;
import com.offbynull.peernetic.actor.helpers.Task;
import com.offbynull.peernetic.overlay.chord.core.ChordState;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class MaintainTask<A> extends AbstractMultiTask {

    private ChordState<A> chordState;
    private Random random;

    public MaintainTask(ChordState<A> chordState, Random random) {
        Validate.notNull(chordState);
        Validate.notNull(random);
        
        this.chordState = chordState;
        this.random = random;
    }
    
    @Override
    protected Set<Task> taskStateUpdated(Set<Task> finished) {
        for (Task task : finished) {
            if (task.getState() == TaskState.FAILED) {
                setFinished(true);
                return Collections.emptySet();
            }
        }
     
        Set<Task> newTasks = new HashSet<>();
        if (finished.isEmpty()) { // initial call
            newTasks.add(new RespondTask(random, chordState));
        }
        
        return newTasks;
    }
    
}
