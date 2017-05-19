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
package com.offbynull.actors.core.gateway.web;

import com.offbynull.actors.core.shuttle.Message;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;

public interface MessageCache {
    void keepAlive(long time, String id);

    void systemToHttpAppend(long time, String id, List<Message> messages);
    void systemToHttpAcknowledge(long time, String id, int maxSeqOffset);
    MessageBlock systemToHttpRead(long time, String id);
    
    void httpToSystemAdd(long time, String id, int seqOffset, List<Message> messages);
    void httpToSystemClear(long time, String id);
    MessageBlock httpToSystemRead(long time, String id);
    
    public static final class MessageBlock {
        private final int startSequenceOffset;
        private final UnmodifiableList<Message> messages;

        public MessageBlock(int startSequenceOffset, List<Message> messages) {
            Validate.isTrue(startSequenceOffset >= 0);
            Validate.notNull(messages);
            Validate.noNullElements(messages);
            this.startSequenceOffset = startSequenceOffset;
            this.messages = (UnmodifiableList<Message>) UnmodifiableList.unmodifiableList(new ArrayList<>(messages));
        }

        public int getStartSequenceOffset() {
            return startSequenceOffset;
        }

        public UnmodifiableList<Message> getMessages() {
            return messages;
        }
    }
}
