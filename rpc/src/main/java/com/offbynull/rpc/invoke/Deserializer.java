package com.offbynull.rpc.invoke;

import org.apache.commons.lang3.Validate;

/**
 * Interface for deserializing method invokations and invokation results.
 * @author Kasra F
 */
public interface Deserializer {
    /**
     * Deserialize method invokation or invokation result.
     * @param data serialized data
     * @return deserialized object
     */
    DeserializerResult deserialize(byte[] data);
    
    /**
     * {@link Deserializer}'s result.
     */
    public static final class DeserializerResult {
        private SerializationType type;
        private Object result;

        /**
         * Constructs a {@link DeserializerResult} object.
         * @param type serialization type
         * @param result resulting object
         * @throws NullPointerException if type is {@code null}, or if {@code type == SerializationType.METHOD_CALL && result == null}
         */
        public DeserializerResult(SerializationType type, Object result) {
            Validate.notNull(type);
            
            if (type == SerializationType.METHOD_CALL && result == null) {
                throw new NullPointerException();
            }
            
            this.type = type;
            this.result = result;
        }

        /**
         * Get the serialization type.
         * @return serialization type
         */
        public SerializationType getType() {
            return type;
        }

        /**
         * Get the deserialized object.
         * @return deserialized object
         */
        public Object getResult() {
            return result;
        }
        
    }
}
