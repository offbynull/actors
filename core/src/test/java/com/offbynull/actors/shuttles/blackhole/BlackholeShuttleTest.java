package com.offbynull.actors.shuttles.blackhole;

import com.offbynull.actors.shuttles.blackhole.BlackholeShuttle;
import com.offbynull.actors.shuttle.Address;
import com.offbynull.actors.shuttle.Message;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;

public class BlackholeShuttleTest {
    
    private BlackholeShuttle fixture;
    
    @Before
    public void setUp() {
        fixture = new BlackholeShuttle("test");
    }

    @Test
    public void mustNotCrash() {
        fixture.send(Arrays.asList(new Message(Address.fromString("src:fake1"), Address.fromString("test:sub1"), "hi1")));
        fixture.send(Arrays.asList(new Message(Address.fromString("src:fake2"), Address.fromString("fake"), "hi2")));
    }

}
