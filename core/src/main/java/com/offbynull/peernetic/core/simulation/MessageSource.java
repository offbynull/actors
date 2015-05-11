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
package com.offbynull.peernetic.core.simulation;

import java.io.IOException;
import java.time.Duration;
import org.apache.commons.lang3.Validate;

/**
 * Reads messages in to a simulation from some external source.
 * @author Kasra Faghihi
 */
public interface MessageSource extends AutoCloseable {
    /**
     * Reads the next message.
     * @return the next message read in, or {@code null} if there are no more messages left to read
     * @throws IOException if an error occurs while reading
     */
    SourceMessage readNextMessage() throws IOException;
    
    /**
     * A message read in by a {@link MessageSource} implementation.
     */
    public static final class SourceMessage {
        private final String source;
        private final String destination;
        private final Duration duration;
        private final Object message;

        /**
         * Constructs a {@link SourceMessage} instance.
         * @param source source address
         * @param destination destination address
         * @param duration amount of time this message waits before arriving
         * @param message message
         * @throws NullPointerException if any argument is {@code null}
         * @throws IllegalArgumentException if {@code duration} is negative
         */
        public SourceMessage(String source, String destination, Duration duration, Object message) {
            Validate.notNull(source);
            Validate.notNull(destination);
            Validate.notNull(duration);
            Validate.notNull(message);
            Validate.isTrue(!duration.isNegative());
            this.source = source;
            this.destination = destination;
            this.duration = duration;
            this.message = message;
        }

        /**
         * Get the source address.
         * @return source address
         */
        public String getSource() {
            return source;
        }

        /**
         * Get the destination address.
         * @return destination address
         */
        public String getDestination() {
            return destination;
        }

        /**
         * Get the duration to wait before sending out the message.
         * @return wait duration
         */
        public Duration getDuration() {
            return duration;
        }

        /**
         * Get the message.
         * @return message
         */
        public Object getMessage() {
            return message;
        }
        
    }
}
