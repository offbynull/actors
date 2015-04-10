package com.offbynull.peernetic.core.gateways.recorder;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;

final class RecordedBlock implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final UnmodifiableList<RecordedMessage> messages;
    private final Instant time;

    public RecordedBlock(List<RecordedMessage> messages, Instant time) {
        Validate.notNull(messages);
        Validate.notNull(time);
        Validate.noNullElements(messages);
        this.messages = (UnmodifiableList<RecordedMessage>) UnmodifiableList.<RecordedMessage>unmodifiableList(new ArrayList<>(messages));
        this.time = time;
    }

    public UnmodifiableList<RecordedMessage> getMessages() {
        return messages;
    }

    public Instant getTime() {
        return time;
    }
    
}
