package com.offbynull.peernetic.common.message;

import org.apache.commons.lang3.Validate;

public interface NonceAccessor<T> {
    
    Nonce<T> get(Object object);
    
    default T getValue(Object object) {
        Validate.notNull(object);
        T nonceValue = InternalUtils.getNonceValue(object);
        return nonceValue;
    }

    default void set(Object object, Nonce<T> nonce) {
        Validate.notNull(object);
        Validate.notNull(nonce);
        setValue(object, nonce.getValue());
    }
    
    default void setValue(Object object, T nonceValue) {
        Validate.notNull(object);
        Validate.notNull(nonceValue);
        InternalUtils.setNonceValue(object, nonceValue);
    }
    
    default boolean containsNonceField(Object object) {
        Validate.notNull(object);
        try {
            InternalUtils.findNonceField(object.getClass());
            return true;
        } catch (IllegalArgumentException iae) {
            return false;
        }
    }

    default boolean containsNonceField(Class<?> cls) {
        Validate.notNull(cls);
        try {
            InternalUtils.findNonceField(cls);
            return true;
        } catch (IllegalArgumentException iae) {
            return false;
        }
    }
}
