package com.offbynull.peernetic.debug.testnetwork;

public final class SimpleSerializerFactory implements SerializerFactory {
    public Serializer createSerializer() {
        return new SimpleSerializer();
    }
}
