package com.offbynull.peernetic.core.actors.reliableproxy;

public interface IdExtractor {
    String getId(Object msg);
}
