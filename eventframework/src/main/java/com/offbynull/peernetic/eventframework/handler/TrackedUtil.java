package com.offbynull.peernetic.eventframework.handler;

import com.offbynull.peernetic.eventframework.processor.ProcessorException;

public final class TrackedUtil {
    private TrackedUtil() {
        
    }

    public static void throwProcessorExceptionOnError(IncomingEvent inEvent,
            long trackedId) {
        
        throwProcessorExceptionOnError(inEvent, trackedId, null);
    }
    
    public static void throwProcessorExceptionOnError(IncomingEvent inEvent,
            long trackedId, Class<? extends ProcessorException> cls) {
        
        if (inEvent instanceof ErrorIncomingEvent) {
            ErrorIncomingEvent eie = (ErrorIncomingEvent) inEvent;

            if (trackedId == eie.getTrackedId()) {
                ProcessorException pe;
                try {
                    pe = cls.newInstance();
                } catch (IllegalAccessException | InstantiationException ex) {
                    pe = new ProcessorException("Intended "
                            + cls.getCanonicalName()
                            + " but unable to create new instance", ex);
                }
                throw pe;
            }
        }
    }
    
    public static boolean isError(IncomingEvent inEvent, long trackedId) {
        
        if (inEvent instanceof ErrorIncomingEvent) {
            ErrorIncomingEvent eie = (ErrorIncomingEvent) inEvent;

            if (trackedId == eie.getTrackedId()) {
                return true;
            }
        }
        
        return false;
    }
    
    public static <T extends TrackedIncomingEvent> T testAndConvert(
            IncomingEvent inEvent, long trackedId, Class<T> toClass) {
        
        if (inEvent instanceof TrackedIncomingEvent) {
            TrackedIncomingEvent tie = (TrackedIncomingEvent) inEvent;

            if (trackedId == tie.getTrackedId()
                    && toClass.isInstance(tie)) {
                return (T) tie;
            }
        }
        
        return null;
    }
}
