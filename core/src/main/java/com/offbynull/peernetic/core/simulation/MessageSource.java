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

public interface MessageSource extends AutoCloseable {
    SourceMessage readNextMessage() throws IOException;
    
    public static final class SourceMessage {
        private final String source;
        private final String destination;
        private final Duration duration;
        private final Object message;

        public SourceMessage(String source, String destination, Duration duration, Object message) {
            Validate.notNull(source);
            Validate.notNull(destination);
            Validate.notNull(duration);
            Validate.notNull(message);
            this.source = source;
            this.destination = destination;
            this.duration = duration;
            this.message = message;
        }

        public String getSource() {
            return source;
        }

        public String getDestination() {
            return destination;
        }

        public Duration getDuration() {
            return duration;
        }

        public Object getMessage() {
            return message;
        }
        
    }
}
