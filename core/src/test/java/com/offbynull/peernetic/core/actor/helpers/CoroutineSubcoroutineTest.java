package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.peernetic.core.simulation.TestHarness;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.mutable.MutableBoolean;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class CoroutineSubcoroutineTest {

    @Test
    public void mustFunnelToCoroutine() {
        MutableBoolean flag = new MutableBoolean();
        
        TestHarness testHarness = new TestHarness("timer");
        testHarness.addCoroutineActor("test", cnt -> {
                CoroutineSubcoroutine fixture = new CoroutineSubcoroutine("wrapper", x -> flag.setValue(true));
                fixture.run(cnt);
            }, Duration.ZERO, Instant.ofEpochMilli(0L), "start");
        
        while (testHarness.hasMore()) {
            testHarness.process();
        }
        
        assertTrue(flag.booleanValue());
    }
    
}
