package com.offbynull.peernetic.core.gateway;

public interface Serializer {
    byte[] serialize(Object obj);
    Object deserialize(byte[] data);
}
