package com.offbynull.peernetic.core.common;

public interface Serializer {
    byte[] serialize(Object obj);
    Object deserialize(byte[] data);
}
