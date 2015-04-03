package com.offbynull.peernetic.core.actors.reliableproxy;

public interface ReceiveGuidelineGenerator {
    ReceiveGuideline generate(Object msg);
}
