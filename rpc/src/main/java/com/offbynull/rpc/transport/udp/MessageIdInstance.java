package com.offbynull.rpc.transport.udp;

import java.net.InetSocketAddress;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

final class MessageIdInstance {
    private InetSocketAddress from;
    private MessageId id;
    private PacketType type;

    public MessageIdInstance(InetSocketAddress from, MessageId id, PacketType type) {
        Validate.notNull(from);
        Validate.notNull(id);
        Validate.notNull(type);
        this.from = from;
        this.id = id;
        this.type = type;
    }

    public InetSocketAddress getFrom() {
        return from;
    }

    public MessageId getId() {
        return id;
    }

    public PacketType getType() {
        return type;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.from);
        hash = 59 * hash + Objects.hashCode(this.id);
        hash = 59 * hash + Objects.hashCode(this.type);
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
        if (this.type != other.type) {
            return false;
        }
        return true;
    }
}
