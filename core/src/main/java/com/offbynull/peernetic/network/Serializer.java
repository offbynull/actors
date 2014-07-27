package com.offbynull.peernetic.network;

public interface Serializer {
    byte[] serialize(Object obj);
    Object deserialize(byte[] data);
}
