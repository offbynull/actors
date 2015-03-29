package com.offbynull.peernetic.actor.network;

public interface Serializer {
    byte[] serialize(Object obj);
    Object deserialize(byte[] data);
}
