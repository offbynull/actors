package com.offbynull.peernetic.common.message;

public final class Validator {
    private Validator() {
        // do nothing
    }
    
    public static boolean validate(Object object) {
        return InternalUtils.validateMessage(object);
    }
}
