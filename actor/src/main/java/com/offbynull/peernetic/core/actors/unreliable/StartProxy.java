package com.offbynull.peernetic.core.actors.unreliable;

import com.offbynull.peernetic.core.common.Serializer;
import org.apache.commons.lang3.Validate;

public class StartProxy {
    private final Serializer serializer;
    private final String timerPrefix;
    private final Line line;

    public StartProxy(String timerPrefix, Line line, Serializer serializer) {
        Validate.notNull(timerPrefix);
        Validate.notNull(line);
        Validate.notNull(serializer);
        this.timerPrefix = timerPrefix;
        this.line = line;
        this.serializer = serializer;
    }

    public String getTimerPrefix() {
        return timerPrefix;
    }

    public Line getLine() {
        return line;
    }

    public Serializer getSerializer() {
        return serializer;
    }
}
