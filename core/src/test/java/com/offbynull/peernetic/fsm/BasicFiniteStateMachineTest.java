package com.offbynull.peernetic.fsm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public final class BasicFiniteStateMachineTest {
    
    @Test
    public void testBasicFiniteStateMachine() {
        Simple simple = new Simple();
        FiniteStateMachine stateMachine = new FiniteStateMachine(simple, Simple.INITIAL_STATE, Object.class);
        
        stateMachine.process(Instant.now(), "0", null);
        stateMachine.setState(Simple.MANUAL_STATE);
        stateMachine.process(Instant.now(), 1, null);
        stateMachine.setState(Simple.MANUAL_TO_AUTO_STATE);
        stateMachine.process(Instant.now(), 2, null);
        stateMachine.setState(Simple.MULTI_STATE_1);
        stateMachine.process(Instant.now(), 3, null);
        stateMachine.setState(Simple.MULTI_STATE_2);
        stateMachine.process(Instant.now(), "4", null);
        stateMachine.setState(Simple.FILTERED_STATE);
        stateMachine.process(Instant.now(), "5", null);
        
        Assert.assertEquals(
                Arrays.asList(
                        "INITIAL-0-PRE",
                        "INITIAL-0",
                        "INITIAL->A",
                        "A-1-PRE",
                        "A-1",
                        "A->B",
                        "B-2-PRE",
                        "B-2",
                        "B->C",
                        "C-2-PRE",
                        "C-2",
                        "C->D",
                        "D-3-PRE",
                        "D-3",
                        "D->E",
                        "E-4-PRE",
                        "E-4",
                        "E->F",
                        "F-5-PREFAIL"),
                simple.getOutput());
    }
    
    private static final class Simple {
        public static final String INITIAL_STATE = "INITIAL";
        public static final String MANUAL_STATE = "A";
        public static final String MANUAL_TO_AUTO_STATE = "B";
        public static final String AUTO_STATE = "C";
        public static final String MULTI_STATE_1 = "D";
        public static final String MULTI_STATE_2 = "E";
        public static final String FILTERED_STATE = "F";
        
        private List<String> output = new ArrayList<>();
        
        @FilterStateHandler({ INITIAL_STATE, MANUAL_STATE, MANUAL_TO_AUTO_STATE, AUTO_STATE, MULTI_STATE_1, MULTI_STATE_2 })
        public void filterHandleAll(String state, FiniteStateMachine fsm, Instant instant, Object message, Object param) {
            output.add(state + "-" + message + "-PRE");
        }

        @FilterStateHandler(FILTERED_STATE)
        public boolean filterHandleFail(String state, FiniteStateMachine fsm, Instant instant, Object message, Object param) {
            output.add(state + "-" + message + "-PREFAIL");
            return false;
        }
        
        @StateHandler(INITIAL_STATE)
        public void handleInitial(String state, FiniteStateMachine fsm, Instant instant, String message, Object param) {
            output.add(state + "-" + message);
        }

        @StateHandler(MANUAL_STATE)
        public void handleManual(String state, FiniteStateMachine fsm, Instant instant, Integer message, Object param) {
            output.add(state + "-" + message);
        }

        @StateHandler(MANUAL_TO_AUTO_STATE)
        public void handleManualToAuto(String state, FiniteStateMachine fsm, Instant instant, Integer message, Object param) {
            output.add(state + "-" + message);
            fsm.switchStateAndProcess(AUTO_STATE, instant, message, null);
        }

        @StateHandler(AUTO_STATE)
        public void handleAuto(String state, FiniteStateMachine fsm, Instant instant, Integer message, Object param) {
            output.add(state + "-" + message);
        }

        @StateHandler({ MULTI_STATE_1, MULTI_STATE_2 })
        public void handleMulti(String state, FiniteStateMachine fsm, Instant instant, Object message, Object param) {
            output.add(state + "-" + message);
        }

        @StateHandler(FILTERED_STATE)
        public void handleFail(String state, FiniteStateMachine fsm, Instant instant, Object message, Object param) {
            throw new IllegalStateException("Should never come in here");
        }
        
        @TransitionHandler({
            @Transition(from = INITIAL_STATE, to = MANUAL_STATE),
            @Transition(from = MANUAL_STATE, to = MANUAL_TO_AUTO_STATE),
            @Transition(from = MANUAL_TO_AUTO_STATE, to = AUTO_STATE),
            @Transition(from = AUTO_STATE, to = MULTI_STATE_1),
            @Transition(from = MULTI_STATE_1, to = MULTI_STATE_2),
            @Transition(from = MULTI_STATE_2, to = FILTERED_STATE),
        })
        public void handleTransitions(String fromState, String toState, FiniteStateMachine fsm) {
            output.add(fromState + "->" + toState);
        }

        public List<String> getOutput() {
            return output;
        }
        
    }
    
}
