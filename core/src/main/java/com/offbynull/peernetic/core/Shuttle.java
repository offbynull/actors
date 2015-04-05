package com.offbynull.peernetic.core;

import java.util.Collection;

public interface Shuttle {
    String getPrefix();
    void send(Collection<Message> messages);
}
