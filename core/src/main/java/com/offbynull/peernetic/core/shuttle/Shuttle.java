package com.offbynull.peernetic.core.shuttle;

import java.util.Collection;

public interface Shuttle {
    String getPrefix();
    void send(Collection<Message> messages);
}
