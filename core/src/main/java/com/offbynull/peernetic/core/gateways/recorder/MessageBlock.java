package com.offbynull.peernetic.core.gateways.recorder;

import com.offbynull.peernetic.core.Message;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;

final class MessageBlock {
    private final UnmodifiableList<Message> messages;
    private final Instant time;

    public MessageBlock(Collection<Message> messages, Instant time) {
        Validate.notNull(messages);
        Validate.notNull(time);
        Validate.noNullElements(messages);
        this.messages = (UnmodifiableList<Message>) UnmodifiableList.<Message>unmodifiableList(new ArrayList<>(messages));
        this.time = time;
    }

    public UnmodifiableList<Message> getMessages() {
        return messages;
    }

    public Instant getTime() {
        return time;
    }
    
}
