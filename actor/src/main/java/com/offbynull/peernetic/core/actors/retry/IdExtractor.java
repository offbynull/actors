package com.offbynull.peernetic.core.actors.retry;

public interface IdExtractor {
    String getId(Object msg);
}
