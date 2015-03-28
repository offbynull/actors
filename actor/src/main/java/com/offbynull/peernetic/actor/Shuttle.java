package com.offbynull.peernetic.actor;

import java.util.Collection;

public interface Shuttle {
    String getPrefix();
    void send(Collection<Message> messages);
}
