package com.offbynull.peernetic.eventframework.processor;

import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.OutgoingEvent;
import com.offbynull.peernetic.eventframework.event.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.processor.ProcessorUtils.OutputValue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

// An adapter that can have multiple processors running at once
public abstract class ProcessorMultiAdapter<N> implements Processor {

    private Set<Processor> processors;

    public ProcessorMultiAdapter() {
    }

    public ProcessorMultiAdapter(Processor... processors) {
        this(Arrays.asList(processors));
    }

    public ProcessorMultiAdapter(Collection<Processor> processors) {
        setProcessor(processors);
    }

    protected final void setProcessor(Collection<Processor> processors) {
        if (processors == null || processors.contains(null)) {
            throw new NullPointerException();
        }

        if (this.processors != null) {
            throw new IllegalArgumentException();
        }

        this.processors = new HashSet<>(processors);
    }

    protected abstract NextAction onResult(Processor proc, Object res)
            throws Exception;

    protected abstract NextAction onException(Processor proc, Exception e)
            throws Exception;

    @Override
    public final ProcessResult process(long timestamp, IncomingEvent event,
            TrackedIdGenerator trackedIdGen) throws Exception {
        Set<Processor> procsToRemove = new HashSet<>();
        List<OutgoingEvent> toBeSent = new LinkedList<>();

        return triggerProcessors(processors, procsToRemove, toBeSent, timestamp,
                event, trackedIdGen);
    }
    
    @SuppressWarnings("unchecked")
    private ProcessResult triggerProcessors(Set<Processor> processors,
            Set<Processor> toBeRemoved, List<OutgoingEvent> toBeSent,
            long timestamp, IncomingEvent event,
            TrackedIdGenerator trackedIdGen) throws Exception {
        
        Set<Processor> procsToAdd = new HashSet<>();

        for (Processor processor : processors) {
            if (toBeRemoved.contains(processor)) {
                continue;
            }
            
            ProcessResult pr;

            try {
                pr = processor.process(timestamp, event, trackedIdGen);
            } catch (Exception e) {
                NextAction nextAction = onException(processor, e);

                if (nextAction == null) {
                    throw new NullPointerException();
                }

                if (nextAction instanceof ProcessorMultiAdapter.ReturnResult) {
                    ReturnResult rr = (ReturnResult) nextAction;
                    return new FinishedProcessResult<>(rr.getResult());
                } else if (nextAction instanceof ProcessorMultiAdapter.ChangeProcessors) {
                    ChangeProcessors cp = (ChangeProcessors) nextAction;
                    toBeRemoved.addAll(cp.viewRemovedProcessors());
                    procsToAdd.removeAll(cp.viewRemovedProcessors());
                    procsToAdd.addAll(cp.viewAddedProcessors());
                    continue;
                } else if (nextAction instanceof ProcessorMultiAdapter.DoNothing) {
                    continue;
                } else {
                    throw new IllegalStateException();
                }
            }

            OutputValue<Boolean> successFlag = new OutputValue<>();
            @SuppressWarnings("unchecked")
            Object res = ProcessorUtils.extractFinishedResult(pr,
                    successFlag);

            if (successFlag.getValue()) {
                NextAction nextAction = onResult(processor, res);

                if (nextAction instanceof ProcessorMultiAdapter.ReturnResult) {
                    ReturnResult rr = (ReturnResult) nextAction;
                    return new FinishedProcessResult<>(rr.getResult());
                } else if (nextAction instanceof ProcessorMultiAdapter.ChangeProcessors) {
                    ChangeProcessors cp = (ChangeProcessors) nextAction;
                    toBeRemoved.addAll(cp.viewRemovedProcessors());
                    procsToAdd.removeAll(cp.viewRemovedProcessors());
                    procsToAdd.addAll(cp.viewAddedProcessors());
                    continue;
                } else if (nextAction instanceof ProcessorMultiAdapter.DoNothing) {
                    continue;
                } else {
                    throw new IllegalStateException();
                }
            }


            toBeSent.addAll(pr.viewOutgoingEvents());
        }
        
        if (!procsToAdd.isEmpty()) {
            triggerProcessors(procsToAdd, toBeRemoved, toBeSent, timestamp,
                    event, trackedIdGen);
            this.processors.addAll(procsToAdd);
        }
        
        this.processors.removeAll(toBeRemoved);
        return new OngoingProcessResult(toBeSent);
    }

    protected interface NextAction {
    }

    protected final class DoNothing implements NextAction {
    }

    protected final class ReturnResult implements NextAction {

        private N res;

        public ReturnResult(N res) {
            this.res = res;
        }

        public N getResult() {
            return res;
        }
    }

    protected final class ChangeProcessors implements NextAction {

        private Set<Processor> addedProcs = new HashSet<>();
        private Set<Processor> removedProcs = new HashSet<>();

        public ChangeProcessors(Collection<Processor> addedProcs,
                Collection<Processor> removedProcs) {
            if (addedProcs == null || removedProcs == null
                    || addedProcs.contains(null)
                    || removedProcs.contains(null)) {
                throw new NullPointerException();
            }

            this.addedProcs.addAll(addedProcs);
            this.removedProcs.addAll(removedProcs);
        }

        public Set<Processor> viewAddedProcessors() {
            return Collections.unmodifiableSet(addedProcs);
        }

        public Set<Processor> viewRemovedProcessors() {
            return Collections.unmodifiableSet(removedProcs);
        }
    }
}
