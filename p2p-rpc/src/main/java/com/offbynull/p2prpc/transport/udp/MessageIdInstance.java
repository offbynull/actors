package com.offbynull.p2prpc.transport.udp;

import java.util.Objects;
import org.apache.commons.lang3.Validate;

final class MessageIdInstance<A> {
    private A from;
    private MessageId id;

    public MessageIdInstance(A from, MessageId id) {
        Validate.notNull(from);
        Validate.notNull(id);
        this.from = from;
        this.id = id;
    }

    public A getFrom() {
        return from;
    }

    public MessageId getId() {
        return id;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + Objects.hashCode(this.from);
        hash = 23 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MessageIdInstance other = (MessageIdInstance) obj;
        if (!Objects.equals(this.from, other.from)) {
            return false;
        }
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }
    
}
