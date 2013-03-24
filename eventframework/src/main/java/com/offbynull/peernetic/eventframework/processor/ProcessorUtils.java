package com.offbynull.peernetic.eventframework.processor;

public final class ProcessorUtils {
    private ProcessorUtils() {
        // do nothing
    }

    @SuppressWarnings("unchecked")
    public static <T> T extractFinishedResult(ProcessResult pr,
            OutputValue<Boolean> successFlag) {
        
        if (pr instanceof FinishedProcessResult) {
            successFlag.setValue(true);
            return (T) ((FinishedProcessResult) pr).getResult();
        }
        
        successFlag.setValue(false);
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T extractFinishedResult(ProcessResult pr,
            T defaultValue) {
        
        if (pr instanceof FinishedProcessResult) {
            return (T) ((FinishedProcessResult) pr).getResult();
        }
        
        return defaultValue;
    }
    
    public static <T> T extractFinishedResult(ProcessResult pr) {
        
        return extractFinishedResult(pr, null);
    }
    
    public static final class OutputValue<T> {
        private T value;

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }
    }
}
