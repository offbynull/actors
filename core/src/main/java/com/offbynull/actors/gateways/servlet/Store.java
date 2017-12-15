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
package com.offbynull.actors.gateways.servlet;

import com.offbynull.actors.shuttle.Message;
import java.io.Closeable;
import java.util.List;

/**
 * Message storage engine for HTTP clients.
 * <p>
 * Each HTTP client ID has 2 queues...
 * <ul>
 * <li>outgoing queue -- messages going to the HTTP client</li>
 * <li>incoming queue -- messages coming from the HTTP client</li>
 * </ul>
 * Both queues have an offset associated with them. The offset is used by HTTP clients to work around network unreliability issues.
 * <p>
 * You can think of queue offsets as auto-incrementing IDs for messages.
 * For example, a HTTP client tries to write messages into the system but experiences network connectivity issues half-way through the HTTP
 * call. The server received and processed those messages, but the HTTP client doesn't know that. As such, the HTTP client will make a new 
 * HTTP call to write those same messages into the system again. But, to prevent duplicate messages going into the system, the HTTP client
 * won't increment the queue offset -- the queue offset for the messages being written in the new HTTP call will be the same as the queue
 * offset in the old HTTP call. Since the server already received and processed the messages for those queue offsets, it'll silently ignore
 * the duplicates.
 * <p>
 * The example above was for an incoming queue (messages coming from the HTTP client), but the same concept applies for an outgoing queue
 * (messages going to the HTTP client). Messages in the outgoing queue won't be dequeued until the HTTP client explicitly asks for the
 * message in the next offset, at which point any messages sitting before that offset will be dequeued.
 * <p>
 * Implementations may choose to have an eviction policy for queues. For example, if messages for a HTTP client haven't been read after some
 * period of time, the implementation may choose to discard the queues.
 * <p>
 * Implementations must be robust. They should only throw exceptions for critical errors. For example, if the implementation encounters
 * connectivity issues, rather than throwing an exception it should block and retry until the issue has been resolved.
 * @author Kasra Faghihi
 */
public interface Store extends Closeable {

    /**
     * Append messages for a HTTP client.
     * <p>
     * Messages are appended to the end of the queue.
     * @param id id of the HTTP client
     * @param messages messages going to {@code id}
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalStateException if the operation could not complete successfully, or if this storage engine has been closed
     */
    void queueOut(String id, List<Message> messages);

    /**
     * Dequeue and return messages for a HTTP client. Messages after and including the message at {@code offset} are returned, while
     * messages before {@code offset} are removed.
     * <p>
     * If {@code offset} points to...
     * <ul>
     * <li>last element + 1, this method will return an empty list.</li>
     * <li>&gt; last element + 1, this method will fail.</li>
     * <li>&lt; first element, this method will fail.</li>
     * </ul>
     * @param id id of the HTTP client
     * @param offset queue offset
     * @return messages after and including the message at {@code offset} coming from {@code id}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code offset < 0}
     * @throws IllegalStateException if message at {@code offset} has already been dequeued, or if message at {@code offset} has yet to be
     * queued, or if the operation could not complete successfully, or if this storage engine has been closed
     */
    List<Message> dequeueOut(String id, int offset);




    /**
     * Insert messages from a HTTP client.
     * <p>
     * If {@code offset} points to...
     * <ul>
     * <li>&gt; last element, this method will fail.</li>
     * <li>&lt;= first element, this method will silently ignore everything up to and including the first element.</li>
     * </ul>
     * @param id id of the HTTP client
     * @param offset queue offset
     * @param messages messages coming from {@code id}
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if {@code offset < 0}
     * @throws IllegalStateException if {@code offset} goes past the end of the queue, or if the operation could not complete successfully,
     * or if this storage engine has been closed
     */
    void queueIn(String id, int offset, List<Message> messages);

    /**
     * Dequeue (and return) messages from a HTTP client.
     * @param id id of the HTTP client
     * @return messages coming from {@code id}
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalStateException if the operation could not complete successfully, or if this storage engine has been closed
     */
    List<Message> dequeueIn(String id);
}