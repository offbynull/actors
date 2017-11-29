/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
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
package com.offbynull.actors.gateways.actor;

import com.offbynull.coroutines.user.CoroutineRunner;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.Validate;

final class Actor {
    private final Actor parent;
    private final Map<String, Actor> children; 
    
    private final CoroutineRunner runner;
    private final Context context;

    Actor(Actor parent, CoroutineRunner runner, Context context) {
        this(parent, runner, new HashMap<>(), context);
    }

    Actor(Actor parent, CoroutineRunner runner, Map<String, Actor> children, Context context) {
        // parent can be null
        Validate.notNull(runner);
        Validate.notNull(context);
        Validate.notNull(children);
        Validate.noNullElements(children.keySet());
        Validate.noNullElements(children.values());
        
        this.parent = parent;
        this.runner = runner;
        this.context = context;
        this.children = children;
    }

    Actor parent() {
        return parent;
    }

    Map<String, Actor> children() {
        return children;
    }

    CoroutineRunner runner() {
        return runner;
    }

    Context context() {
        return context;
    }

    boolean isRoot() {
        return parent == null;
    }

    Actor getChild(String id) {
        Validate.notNull(id);
        return children.get(id);
    }
    
    boolean isChild(String id) {
        Validate.notNull(id);
        return children.containsKey(id);
    }
    
}
