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
package com.offbynull.peernetic.core.gateways.recorder;

import com.offbynull.peernetic.core.shuttle.Message;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;

final class MessageBlock {
    private final UnmodifiableList<Message> messages;
    private final Instant time;

    public MessageBlock(Collection<Message> messages, Instant time) {
        Validate.notNull(messages);
        Validate.notNull(time);
        Validate.noNullElements(messages);
        this.messages = (UnmodifiableList<Message>) UnmodifiableList.<Message>unmodifiableList(new ArrayList<>(messages));
        this.time = time;
    }

    public UnmodifiableList<Message> getMessages() {
        return messages;
    }

    public Instant getTime() {
        return time;
    }
    
}
