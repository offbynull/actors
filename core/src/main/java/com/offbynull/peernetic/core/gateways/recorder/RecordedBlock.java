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

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;

// public because other tools may want to read out recorded data
/**
 * A block of recorded message. May contain zero or more {@link RecordedMessage}s and the time in which those messages were sent. Publicly
 * exposed because other tools may want to read out messages written by {@link RecorderGateway}, or write messages to be read by
 * {@link ReplayerGateway}.
 * @author Kasra Faghihi
 */
public final class RecordedBlock implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final UnmodifiableList<RecordedMessage> messages;
    private final Instant time;

    /**
     * Constructs a {@link RecordedBlock} object.
     * @param messages messages in block
     * @param time time which messages in this block were recorded
     */
    public RecordedBlock(List<RecordedMessage> messages, Instant time) {
        Validate.notNull(messages);
        Validate.notNull(time);
        Validate.noNullElements(messages);
        this.messages = (UnmodifiableList<RecordedMessage>) UnmodifiableList.<RecordedMessage>unmodifiableList(new ArrayList<>(messages));
        this.time = time;
    }

    /**
     * Get messages within block.
     * @return messages
     */
    public UnmodifiableList<RecordedMessage> getMessages() {
        return messages;
    }

    /**
     * Get time which messages in this block were recorded.
     * @return time which messages in this block were recorded
     */
    public Instant getTime() {
        return time;
    }
    
}
