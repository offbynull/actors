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
 * Message storage engine. Used to queue messages going to HTTP clients.
 * <p>
 * Implementations may choose to have an eviction policy for messages. For example, if messages for a HTTP client haven't been read after
 * some period of time, an implementation may choose to discard the messages.
 * <p>
 * Implementations must be robust. It should only throw exceptions for critical errors. For example, if the implementation encounters
 * connectivity issues, rather than throwing an exception it should block and retry until the issue has been resolved.
 * @author Kasra Faghihi
 */
public interface Store extends Closeable {

    /**
     * Puts messages for a HTTP client into storage.
     * @param id id of the HTTP client
     * @param messages messages going to {@code id}
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if any of the messages are for an invalid destination address (bad prefix or unexpected size)
     * @throws IllegalStateException if the operation could not complete successfully, or if this storage engine has been closed
     */
    void write(String id, List<Message> messages);

    /**
     * Reads (and removes) messages for a HTTP client from storage.
     * @param id id of the HTTP client
     * @return messages going to {@code id}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if the operation could not complete successfully, or if this storage engine has been closed
     */
    List<Message> read(String id);
}
