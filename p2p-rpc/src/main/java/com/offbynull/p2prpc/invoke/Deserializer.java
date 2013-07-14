package com.offbynull.p2prpc.invoke;

public interface Deserializer {
    DeserializerResult deserialize(byte[] data);
    
    public static final class DeserializerResult {
        private SerializationType type;
        private Object result;

        public DeserializerResult(SerializationType type, Object result) {
            this.type = type;
            this.result = result;
        }

        public SerializationType getType() {
            return type;
        }

        public Object getResult() {
            return result;
        }
        
    }
}
