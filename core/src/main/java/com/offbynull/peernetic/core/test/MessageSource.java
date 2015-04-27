package com.offbynull.peernetic.core.test;

import java.io.IOException;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public interface MessageSource extends AutoCloseable {
    SourceMessage readNextMessage() throws IOException;
    
    public static final class SourceMessage {
        private final String source;
        private final String destination;
        private final Instant time;
        private final Object message;

        public SourceMessage(String source, String destination, Instant time, Object message) {
            Validate.notNull(source);
            Validate.notNull(destination);
            Validate.notNull(time);
            Validate.notNull(message);
            this.source = source;
            this.destination = destination;
            this.time = time;
            this.message = message;
        }

        public String getSource() {
            return source;
        }

        public String getDestination() {
            return destination;
        }

        public Instant getTime() {
            return time;
        }

        public Object getMessage() {
            return message;
        }
        
    }
}
