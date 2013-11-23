package com.offbynull.rpc.invoke;

import org.apache.commons.lang3.Validate;

public interface Deserializer {
    DeserializerResult deserialize(byte[] data);
    
    public static final class DeserializerResult {
        private SerializationType type;
        private Object result;

        public DeserializerResult(SerializationType type, Object result) {
            Validate.notNull(type);
            
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
