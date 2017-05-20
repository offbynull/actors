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

/**
 * Message cache for servlet gateway.
 * <p>
 * The message cache buffers incoming and outgoing messages, as well as provides some mitigation to the volatility that comes with having
 * a network connection.
 * <p>
 * For example, the HTTP client may try to send a message but the underlying connection gets ungracefully terminated mid-stream. The server
 * may have received the message, but the client doesn't know for sure. It can try resending the message, but then the server may get the
 * same message twice.
 * <p>
 * The message cache mitigates this problem by introducing sequence numbers. A message has a sequence number associated with it, so if the
 * connection cuts out the client can resend that message with the <b>same sequence number</b> and the server would know that it already
 * got that message. The same concept applies with server to client messages.
 * <p>
 * Implementations of this class must be thread-safe.
 * @author Kasra Faghihi
 */
public interface MessageCache {

    /**
     * Updates last access time for {@code id}.
     * <p>
     * This method should be invoked when an HTTP client connects. If an entry doesn't exist for {@code id}, one will be created. If it does
     * exist, it will be updated.
     * @param id HTTP client id
     * @throws NullPointerException if any argument is {@code null}
     */
    void keepAlive(String id);



    /**
     * Buffer messages intended for some HTTP client (from the actor system).
     * @param id HTTP client id
     * @param messages messages
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if {@code id} is not tracked (e.g. timed out)
     */
    void systemToHttpAppend(String id, List<Message> messages);
    
    /**
     * Acknowledge that messages sent to some HTTP client have been received.
     * @param id HTTP client id
     * @param maxSeqOffset largest sequence number being acknowledged (that message will be acknowledged as well as all messages before it)
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if {@code maxSeqOffset} is negative, or if {@code id} is not tracked (e.g. timed out)
     */
    void systemToHttpAcknowledge(String id, int maxSeqOffset);

    /**
     * Read unacknowledged messages intended for some HTTP client.
     * @param id HTTP client id
     * @return buffered messages
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code id} is not tracked (e.g. timed out)
     */
    MessageBlock systemToHttpRead(String id);



    /**
     * Buffer messages intended for the actor system (from the HTTP client).
     * @param id HTTP client id
     * @param seqOffset sequence number at which {@code messages} starts
     * @param messages messages
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if {@code id} is not tracked (e.g. timed out)
     */    
    void httpToSystemAdd(String id, int seqOffset, List<Message> messages);
    
    /**
     * Acknowledge that messages intended for the actor system have been dispatched.
     * @param id HTTP client id
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code id} is not tracked (e.g. timed out)
     */
    void httpToSystemClear(String id);

    /**
     * Read unacknowledged messages intended for the actor system.
     * @param id HTTP client id
     * @return buffered messages
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code id} is not tracked (e.g. timed out)
     */
    MessageBlock httpToSystemRead(String id);

    /**
     * A block of messages with a sequence offset.
     */
    public static final class MessageBlock {
        private final int startSequenceOffset;
        private final UnmodifiableList<Message> messages;

        /**
         * Constructs a {@link MessageBlock} object.
         * @param startSequenceOffset sequence number at which {@code messages} starts
         * @param messages messages
         * @throws NullPointerException if any argument is {@code null} or contains {@code null}
         * @throws IllegalArgumentException if {@code startSequenceOffset} is negative
         */
        public MessageBlock(int startSequenceOffset, List<Message> messages) {
            Validate.isTrue(startSequenceOffset >= 0);
            Validate.notNull(messages);
            Validate.noNullElements(messages);
            this.startSequenceOffset = startSequenceOffset;
            this.messages = (UnmodifiableList<Message>) UnmodifiableList.unmodifiableList(new ArrayList<>(messages));
        }

        /**
         * Get sequence number at which messages in this block start.
         * @return sequence number
         */
        public int getStartSequenceOffset() {
            return startSequenceOffset;
        }

        /**
         * Get messages.
         * @return messages
         */
        public UnmodifiableList<Message> getMessages() {
            return messages;
        }
    }
}
