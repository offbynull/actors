package com.offbynull.peernetic.core.simulation;

import java.time.Duration;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
public class SimpleMessageBehaviourDriverTest {
    
    private SimpleMessageBehaviourDriver fixture;
    
    @Before
    public void setUp() {
        fixture = new SimpleMessageBehaviourDriver();
    }

    @Test
    public void mustAlwaysReturnDurationOfZero() {
        Duration duration = fixture.calculateDuration("test", "test", "test");
        assertEquals(Duration.ZERO, duration);
    }
    
}
