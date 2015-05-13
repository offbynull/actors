package com.offbynull.peernetic.core.simulation;

import java.time.Duration;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
public class SimpleMessageBehaviourDriverTest {
    
    private SimpleMessageDurationCalculator fixture;
    
    @Before
    public void setUp() {
        fixture = new SimpleMessageDurationCalculator();
    }

    @Test
    public void mustAlwaysReturnDurationOfZero() {
        Duration duration = fixture.calculateDuration("test", "test", "test");
        assertEquals(Duration.ZERO, duration);
    }
    
}
