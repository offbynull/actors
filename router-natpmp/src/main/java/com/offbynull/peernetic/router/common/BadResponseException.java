package com.offbynull.peernetic.router.common;

public final class BadResponseException extends RuntimeException {

    public BadResponseException() {
    }

    public BadResponseException(String message) {
        super(message);
    }

    public BadResponseException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadResponseException(Throwable cause) {
        super(cause);
    }
    
}
