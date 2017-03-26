package com.offbynull.actors.core.simulator;

import com.offbynull.actors.core.simulator.SimpleActorDurationCalculator;
import com.offbynull.actors.core.shuttle.Address;
import java.time.Duration;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
public class SimpleActorDurationCalculatorTest {
    
    private SimpleActorDurationCalculator fixture;
    
    @Before
    public void setUp() {
        fixture = new SimpleActorDurationCalculator();
    }

    @Test
    public void mustAlwaysReturnDurationOfZero() {
        Duration duration = fixture.calculateDuration(Address.of("test"), Address.of("test"), "test", Duration.ofSeconds(5L));
        assertEquals(Duration.ZERO, duration);
    }
    
}
