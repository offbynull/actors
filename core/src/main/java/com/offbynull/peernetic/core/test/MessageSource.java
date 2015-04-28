package com.offbynull.peernetic.core.test;

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
