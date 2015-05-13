package com.offbynull.peernetic.core.simulator;

import com.offbynull.peernetic.core.simulator.SimpleActorDurationCalculator;
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
        Duration duration = fixture.calculateDuration("test", "test", "test", Duration.ofSeconds(5L));
        assertEquals(Duration.ZERO, duration);
    }
    
}
