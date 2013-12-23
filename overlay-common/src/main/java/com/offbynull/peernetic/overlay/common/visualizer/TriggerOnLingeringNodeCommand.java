/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.overlay.common.visualizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * Waits for a node to have no edges to/from it and then triggers a set of commands. Applies to the node AT THE TIME this command was
 * issued... That is, if this command gets issued and the node it refers to is removed then added again, this command will have no effect.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class TriggerOnLingeringNodeCommand<A> implements Command<A> {
    private A node;
    private List<Command<A>> triggerCommand;

    /**
     * Constructs a {@link TriggerOnLingeringNodeCommand} object.
     * @param node node
     * @param triggerCommand commands to trigger
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     */
    public TriggerOnLingeringNodeCommand(A node, Command<A> ... triggerCommand) {
        Validate.notNull(node);
        Validate.noNullElements(triggerCommand);
        
        this.node = node;
        this.triggerCommand = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(triggerCommand)));
    }

    /**
     * Get node.
     * @return node
     */
    public A getNode() {
        return node;
    }

    /**
     * Get trigger commands.
     * @return trigger commands
     */
    public List<Command<A>> getTriggerCommand() {
        return triggerCommand;
    }

    
}
