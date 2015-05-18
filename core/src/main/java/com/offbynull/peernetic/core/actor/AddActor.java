/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.core.actor;

import java.util.Arrays;
import org.apache.commons.collections4.collection.UnmodifiableCollection;
import org.apache.commons.lang3.Validate;

final class AddActor {
    private final String id;
    private final Actor actor;
    private final UnmodifiableCollection<Object> primingMessages;

    public AddActor(String id, Actor actor, Object... primingMessages) {
        Validate.notNull(id);
        Validate.notNull(actor);
        Validate.notNull(primingMessages);
        Validate.noNullElements(primingMessages);
        this.id = id;
        this.actor = actor;
        this.primingMessages = (UnmodifiableCollection<Object>) UnmodifiableCollection.<Object>unmodifiableCollection(
                Arrays.asList(primingMessages));
    }

    public String getId() {
        return id;
    }

    public Actor getActor() {
        return actor;
    }

    public UnmodifiableCollection<Object> getPrimingMessages() {
        return primingMessages;
    }

    @Override
    public String toString() {
        return "AddActor{" + "id=" + id + ", actor=" + actor + ", primingMessages=" + primingMessages + '}';
    }
    
}
