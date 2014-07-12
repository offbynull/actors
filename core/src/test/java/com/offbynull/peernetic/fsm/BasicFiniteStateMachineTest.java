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
        
        Assert.assertEquals(
                Arrays.asList("INITIAL-0", "INITIAL->A", "A-1", "A->B", "B-2", "B->C", "C-2", "C->D", "D-3", "D->E", "E-4"),
                simple.getOutput());
    }
    
    private static final class Simple {
        public static final String INITIAL_STATE = "INITIAL";
        public static final String MANUAL_STATE = "A";
        public static final String MANUAL_TO_AUTO_STATE = "B";
        public static final String AUTO_STATE = "C";
        public static final String MULTI_STATE_1 = "D";
        public static final String MULTI_STATE_2 = "E";
        
        private List<String> output = new ArrayList<>();
        
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
        
        @TransitionHandler({
            @Transition(from = INITIAL_STATE, to = MANUAL_STATE),
            @Transition(from = MANUAL_STATE, to = MANUAL_TO_AUTO_STATE),
            @Transition(from = MANUAL_TO_AUTO_STATE, to = AUTO_STATE),
            @Transition(from = AUTO_STATE, to = MULTI_STATE_1),
            @Transition(from = MULTI_STATE_1, to = MULTI_STATE_2),
        })
        public void handleTransitions(String fromState, String toState, FiniteStateMachine fsm) {
            output.add(fromState + "->" + toState);
        }

        public List<String> getOutput() {
            return output;
        }
        
    }
    
}
