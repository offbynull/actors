package com.offbynull.overlay.visualizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.Validate;

//applies to the node AT THE TIME this command gets issues... that is, if the node is removed and then added again, this command will have
// no effect
public final class TriggerOnLingeringNodeCommand<A> implements Command<A> {
    private A node;
    private List<Command<A>> triggerCommand;

    public TriggerOnLingeringNodeCommand(A node, Command<A> ... triggerCommand) {
        Validate.notNull(node);
        Validate.noNullElements(triggerCommand);
        
        this.node = node;
        this.triggerCommand = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(triggerCommand)));
    }

    public A getNode() {
        return node;
    }

    public List<Command<A>> getTriggerCommand() {
        return triggerCommand;
    }

    
}
