package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.peernetic.core.simulation.Simulator;
import java.time.Duration;
import java.time.Instant;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SleepSubcoroutineTest {
    
    @Test
    public void mustSleepFor1Second() throws Exception {
        Simulator testHarness = new Simulator();
        testHarness.addTimer("timer", Instant.ofEpochMilli(0L));
        testHarness.addCoroutineActor("test", cnt -> {
                SleepSubcoroutine fixture = new SleepSubcoroutine.Builder()
                        .id("sleep")
                        .timerAddressPrefix("timer")
                        .duration(Duration.ofSeconds(1L))
                        .build();
                fixture.run(cnt);
            }, Duration.ZERO, Instant.ofEpochMilli(0L), "start");
        
        Instant time = null;
        while (testHarness.hasMore()) {
            time = testHarness.process();
        }
        
        assertEquals(1000L, time.toEpochMilli());
    }
    
}
