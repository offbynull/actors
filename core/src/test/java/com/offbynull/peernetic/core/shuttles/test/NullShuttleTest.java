package com.offbynull.peernetic.core.shuttles.test;

import com.offbynull.peernetic.core.shuttle.Message;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;

public class NullShuttleTest {
    
    private NullShuttle fixture;
    
    @Before
    public void setUp() {
        fixture = new NullShuttle("test");
    }

    @Test
    public void mustNotCrash() {
        fixture.send(Arrays.asList(new Message("src:fake1", "test:sub1", "hi1")));
        fixture.send(Arrays.asList(new Message("src:fake2", "fake", "hi2")));
    }

}
