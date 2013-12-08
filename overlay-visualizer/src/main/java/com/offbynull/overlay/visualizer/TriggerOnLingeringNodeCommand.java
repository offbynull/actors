package com.offbynull.overlay.visualizer;

import org.apache.commons.lang3.Validate;

//applies to the node AT THE TIME this command gets issues... that is, if the node is removed and then added again, this command will have
// no effect
public final class TriggerOnLingeringNodeCommand<A> implements Command<A> {
    private A node;
    private Command<A> triggerCommand;

    public TriggerOnLingeringNodeCommand(A node, Command<A> triggerCommand) {
        Validate.notNull(node);
        Validate.notNull(triggerCommand);
        
        this.node = node;
        this.triggerCommand = triggerCommand;
    }
    
}
