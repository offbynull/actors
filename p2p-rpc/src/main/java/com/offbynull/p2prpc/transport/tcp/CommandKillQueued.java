package com.offbynull.p2prpc.transport.tcp;

final class CommandKillQueued implements Command {
    private long id;

    CommandKillQueued(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }
    
}
