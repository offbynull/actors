package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.simulator.Simulator;
import java.time.Duration;
import java.time.Instant;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SleepSubcoroutineTest {
    
    @Test
    public void mustSleepFor1Second() throws Exception {
        Simulator testHarness = new Simulator();
        testHarness.addTimer("timer", Instant.ofEpochMilli(0L));
        testHarness.addActor("test", (Continuation cnt) -> {
                SleepSubcoroutine fixture = new SleepSubcoroutine.Builder()
                        .sourceAddress(Address.of("sleep"))
                        .timerAddress(Address.of("timer"))
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
