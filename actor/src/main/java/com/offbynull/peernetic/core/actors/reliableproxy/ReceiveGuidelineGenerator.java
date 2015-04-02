package com.offbynull.peernetic.core.actors.reliableproxy;

public interface ReceiveGuidelineGenerator {
    SendGuideline generate(Object msg);
}
