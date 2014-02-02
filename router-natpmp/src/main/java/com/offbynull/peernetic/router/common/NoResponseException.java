package com.offbynull.peernetic.router.common;

public final class NoResponseException extends RuntimeException {

    public NoResponseException() {
    }

    public NoResponseException(String message) {
        super(message);
    }

    public NoResponseException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoResponseException(Throwable cause) {
        super(cause);
    }
    
}
