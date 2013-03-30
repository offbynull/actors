package com.offbynull.peernetic.eventframework.processor;

import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.processor.ProcessorUtils.OutputValue;

public abstract class ProcessorChainAdapter<N> implements Processor {

    private Processor processor;

    public ProcessorChainAdapter() {
    }

    public ProcessorChainAdapter(Processor processor) {
        if (processor == null) {
            throw new NullPointerException();
        }
        
        this.processor = processor;
    }

    protected final void setProcessor(Processor processor) {
        if (processor == null) {
            throw new NullPointerException();
        }
        
        if (this.processor != null) {
            throw new IllegalArgumentException();
        }
        
        this.processor = processor;
    }
    
    protected abstract NextAction onResult(Processor proc, Object res)
            throws Exception;
    
    protected abstract NextAction onException(Processor proc, Exception e)
            throws Exception;
    
    @Override
    @SuppressWarnings("unchecked")
    public final ProcessResult process(long timestamp, IncomingEvent event,
            TrackedIdGenerator trackedIdGen) throws Exception {
        while (true) {
            ProcessResult pr;
            try {
                pr = processor.process(timestamp, event, trackedIdGen);
            } catch (Exception e) {
                NextAction nextAction = onException(processor, e);
                
                if (nextAction == null) {
                    throw new NullPointerException();
                }
                
                if (nextAction
                        instanceof ProcessorChainAdapter.ReturnResult) {
                    ReturnResult rr = (ReturnResult) nextAction;
                    return new FinishedProcessResult<>(rr.getResult());
                } else if (nextAction
                        instanceof ProcessorChainAdapter.GoToNextProcessor) {
                    GoToNextProcessor gtnp = (GoToNextProcessor) nextAction;
                    processor = gtnp.getProcessor();
                    continue;
                } else {
                    throw new IllegalStateException();
                }
            }

            OutputValue<Boolean> successFlag = new OutputValue<>();
            @SuppressWarnings("unchecked")
            Object res = ProcessorUtils.extractFinishedResult(pr, successFlag);

            if (successFlag.getValue()) {
                NextAction nextAction = onResult(processor, res);
                
                if (nextAction
                        instanceof ProcessorChainAdapter.ReturnResult) {
                    ReturnResult rr = (ReturnResult) nextAction;
                    return new FinishedProcessResult<>(rr.getResult());
                } else if (nextAction
                        instanceof ProcessorChainAdapter.GoToNextProcessor) {
                    GoToNextProcessor gtnp = (GoToNextProcessor) nextAction;
                    processor = gtnp.getProcessor();
                    continue;
                } else {
                    throw new IllegalStateException();
                }
            }

            return pr;
        }
    }
    
    protected interface NextAction { }
    
    protected final class ReturnResult implements NextAction {
        private N res;

        public ReturnResult(N res) {
            this.res = res;
        }

        public N getResult() {
            return res;
        }
        
    }
    
    protected final class GoToNextProcessor implements NextAction {
        private Processor proc;

        public GoToNextProcessor(Processor proc) {
            if (proc == null) {
                throw new NullPointerException();
            }
            this.proc = proc;
        }

        public Processor getProcessor() {
            return proc;
        }
        
    }
}
