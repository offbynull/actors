package com.offbynull.peernetic.core.actors.unreliable;

import org.apache.commons.lang3.Validate;

public class StartUnreliableProxy {
    private final String timerPrefix;
    private final String actorPrefix;
    private final Line line;

    public StartUnreliableProxy(String timerPrefix, String actorPrefix, Line line) {
        Validate.notNull(timerPrefix);
        Validate.notNull(line);
        this.timerPrefix = timerPrefix;
        this.actorPrefix = actorPrefix;
        this.line = line;
    }

    public String getTimerPrefix() {
        return timerPrefix;
    }

    public String getActorPrefix() {
        return actorPrefix;
    }

    public Line getLine() {
        return line;
    }
}
