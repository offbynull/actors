package com.offbynull.peernetic.chord.processors;

import com.offbynull.peernetic.chord.FingerTable;
import com.offbynull.peernetic.chord.Id;
import com.offbynull.peernetic.chord.Pointer;
import com.offbynull.peernetic.chord.processors.RouteProcessor.RouteProcessorResult;
import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.processor.ProcessorChainAdapter;
import com.offbynull.peernetic.eventframework.processor.ProcessorException;

public final class FixFingerProcessor extends ProcessorChainAdapter<Boolean> {

    private FingerTable fingerTable;
    private int index;
    private Pointer testPtr;
    private State state;

    public FixFingerProcessor(FingerTable fingerTable, int index) {
        if (fingerTable == null) {
            throw new NullPointerException();
        }

        if (index < 0 || index >= fingerTable.getBaseId().getBitCount()) {
            throw new IllegalArgumentException();
        }

        this.fingerTable = fingerTable;
        this.index = index;

        state = State.TESTING_INIT;

        testPtr = fingerTable.get(index);
        Processor proc = new QueryForFingerTableProcessor(testPtr.getAddress());
        setProcessor(proc);
    }

    @Override
    protected NextAction onResult(Processor proc, Object res) throws Exception {
        switch (state) {
            case TESTING_INIT:
                return processTestingInit();
            case SCANNING:
                return processScanningResult(res);
            case TESTING_SUCCESSOR:
                return processTestingSuccessorResult(res);
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    protected NextAction onException(Processor proc, Exception e)
            throws Exception {
        if (e instanceof FixFingerFailedException) {
            throw e;
        }
        
        switch (state) {
            case TESTING_INIT:
                // finger node failed to respond to test, so remove it before
                // moving on to update
                fingerTable.remove(testPtr);
                return processTestingInit();
            case SCANNING:
            case TESTING_SUCCESSOR:
                return new ReturnResult(false);
            default:
                throw new IllegalStateException();
        }
    }

    private NextAction processTestingInit() throws Exception {
        Id destId = fingerTable.getExpectedId(index);
        Id baseId = fingerTable.getBaseId();

        Pointer bootstrap = fingerTable.get(0);
        Id bootstrapId = bootstrap.getId();

        if (destId.isWithin(baseId, true, bootstrapId, false)) {
            return new ReturnResult(false);
        }

        state = State.SCANNING;
        
        Processor nextProc = new RouteProcessor(baseId, destId,
                bootstrap.getAddress());
        return new GoToNextProcessor(nextProc);
    }

    private NextAction processScanningResult(Object res) {
        RouteProcessorResult rpRes = (RouteProcessorResult) res;
        Pointer pred = rpRes.getFound();
        
        state = State.TESTING_SUCCESSOR;
        
        Processor proc = new QueryForFingerTableProcessor(pred.getAddress());
        return new GoToNextProcessor(proc);
    }

    private NextAction processTestingSuccessorResult(Object res) {
        FingerTable ftRes = (FingerTable) res;
        fingerTable.put(ftRes.getBase());
        return new ReturnResult(true);
    }

    private enum State {

        TESTING_INIT,
        SCANNING,
        TESTING_SUCCESSOR,
        FINISHED
    }

    public static final class FixFingerFailedException
            extends ProcessorException {
    }
}
