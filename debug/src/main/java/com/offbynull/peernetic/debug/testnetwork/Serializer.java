package com.offbynull.peernetic.debug.testnetwork;

public interface Serializer {
    byte[] serialize(Object message);
    Object deserialize(byte[] buffer);
}
