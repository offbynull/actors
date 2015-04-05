package com.offbynull.peernetic.core.actors.retry;

public interface ReceiveGuidelineGenerator {
    ReceiveGuideline generate(Object msg);
}
