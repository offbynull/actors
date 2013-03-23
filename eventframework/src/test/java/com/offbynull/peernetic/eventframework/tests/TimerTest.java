package com.offbynull.peernetic.eventframework.tests;

import com.offbynull.peernetic.eventframework.BlockingClientResultListener;
import com.offbynull.peernetic.eventframework.BlockingClientResultListener.ResultType;
import com.offbynull.peernetic.eventframework.Client;
import com.offbynull.peernetic.eventframework.handler.Handler;
import com.offbynull.peernetic.eventframework.impl.lifecycle.LifecycleHandler;
import com.offbynull.peernetic.eventframework.impl.timer.TimerHandler;
import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.simplifier.IncomingSimplifier;
import com.offbynull.peernetic.eventframework.simplifier.OutgoingSimplifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TimerTest {

    public TimerTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void waitTest() throws InterruptedException {
        Set<Handler> handlers = new HashSet<>();
        handlers.add(new LifecycleHandler());
        handlers.add(new TimerHandler());
        Processor processor = new TimerProcessor(500L);
        BlockingClientResultListener resultListener =
                new BlockingClientResultListener();

        Client client = new Client(processor, resultListener, handlers,
                Collections.<OutgoingSimplifier>emptySet(),
                Collections.<IncomingSimplifier>emptySet());
        client.start();
        
        resultListener.waitForResult();
        
        assertEquals(ResultType.FINISHED, resultListener.getResultType());
    
        client.stop();
    }
}
