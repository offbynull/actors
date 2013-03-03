package com.offbynull.peernetic.eventframework.processor;

public class ProcessorException extends RuntimeException {

    public ProcessorException() {
    }

    public ProcessorException(String message) {
        super(message);
    }

    public ProcessorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProcessorException(Throwable cause) {
        super(cause);
    }

}
