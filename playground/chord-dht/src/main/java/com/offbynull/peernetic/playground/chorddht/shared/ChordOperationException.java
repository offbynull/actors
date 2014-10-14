package com.offbynull.peernetic.playground.chorddht.shared;

import com.offbynull.peernetic.common.skeleton.SimpleJavaflowTask;
import org.apache.commons.lang3.Validate;

public class ChordOperationException extends RuntimeException {

    ChordOperationException(Object request, Class<?> responseType) {
        super("No response of type " + responseType + " for request " + request);
        Validate.notNull(request);
        Validate.notNull(responseType);
    }

    ChordOperationException(Class<? extends SimpleJavaflowTask> task) {
        super("Task " + task + " failed");
        Validate.notNull(task);
    }

    ChordOperationException(String message) {
        super(message);
    }
    
}
