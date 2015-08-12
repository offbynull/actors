package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.simulator.Simulator;
import java.time.Duration;
import java.time.Instant;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

public class SubcoroutineStepperTest {

    @Test
    public void mustFullyExecuteSubcoroutine() {
        
        final int stepCount = 5;
        
        Simulator testHarness = new Simulator();
        testHarness.addActor("test", (Continuation cnt) -> {
            Context ctx = (Context) cnt.getContext();

            Subcoroutine<Integer> testSubcoroutine = new LoopSubcoroutine(stepCount);
            SubcoroutineStepper<Integer> fixture = new SubcoroutineStepper<>(ctx, testSubcoroutine);
            
            for (int i = 0; i < stepCount; i++) {
                assertTrue(fixture.step());
            }
            assertFalse(fixture.step());
            
            assertEquals(stepCount, (int) fixture.getResult());
        }, Duration.ZERO, Instant.ofEpochMilli(0L), "start");

        while (testHarness.hasMore()) {
            testHarness.process();
        }
    }
    
    @Test
    public void mustFailToOverExecuteSubcoroutine() {
        
        final int stepCount = 5;
        
        Simulator testHarness = new Simulator();
        testHarness.addActor("test", (Continuation cnt) -> {
            Context ctx = (Context) cnt.getContext();

            Subcoroutine<Integer> testSubcoroutine = new LoopSubcoroutine(stepCount);
            SubcoroutineStepper<Integer> fixture = new SubcoroutineStepper<>(ctx, testSubcoroutine);
            
            for (int i = 0; i < stepCount; i++) {
                assertTrue(fixture.step());
            }
            assertFalse(fixture.step());
            
            // ExceptedException rule doesn't calling step on a finished subcoroutine -- exception at this point
            try {
                fixture.step();
                fail();
            } catch (IllegalStateException ise) {
                // do nothing
            }
        }, Duration.ZERO, Instant.ofEpochMilli(0L), "start");

        while (testHarness.hasMore()) {
            testHarness.process();
        }
    }

    @Test
    public void mustFailToGetResultIfSubcoroutineHasNotFinished() {
        
        final int stepCount = 5;
        
        Simulator testHarness = new Simulator();
        testHarness.addActor("test", (Continuation cnt) -> {
            Context ctx = (Context) cnt.getContext();

            Subcoroutine<Integer> testSubcoroutine = new LoopSubcoroutine(stepCount);
            SubcoroutineStepper<Integer> fixture = new SubcoroutineStepper<>(ctx, testSubcoroutine);

            assertTrue(fixture.step());
            
            // ExceptedException rule doesn't calling step on a finished subcoroutine -- exception at this point
            try {
                fixture.getResult();
                fail();
            } catch (IllegalStateException ise) {
                // do nothing
            }
        }, Duration.ZERO, Instant.ofEpochMilli(0L), "start");

        while (testHarness.hasMore()) {
            testHarness.process();
        }
    }
    
    private static final class LoopSubcoroutine implements Subcoroutine<Integer> {

        private final int loopLimit;

        public LoopSubcoroutine(int loopLimit) {
            this.loopLimit = loopLimit;
        }
        
        @Override
        public Integer run(Continuation cnt) throws Exception {
            for (int i = 0; i < loopLimit; i++) {
                cnt.suspend();
            }
            
            return loopLimit;
        }
        
        @Override
        public Address getAddress() {
            return Address.of();
        }
    }

}
