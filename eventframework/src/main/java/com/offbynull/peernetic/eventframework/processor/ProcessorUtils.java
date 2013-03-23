package com.offbynull.peernetic.eventframework.processor;

public final class ProcessorUtils {
    private ProcessorUtils() {
        // do nothing
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T extractFinishedResult(ProcessResult pr) {
        
        if (pr instanceof FinishedProcessResult) {
            return (T) ((FinishedProcessResult) pr).getResult();
        }
        
        return null;
    }
}
