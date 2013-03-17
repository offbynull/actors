package com.offbynull.peernetic.eventframework.tests;

import com.offbynull.peernetic.eventframework.tests.BlockingClientResultListener.ResultType;
import com.offbynull.peernetic.eventframework.Client;
import com.offbynull.peernetic.eventframework.handler.Handler;
import com.offbynull.peernetic.eventframework.handler.communication.CommunicationHandler;
import com.offbynull.peernetic.eventframework.handler.lifecycle.LifecycleHandler;
import com.offbynull.peernetic.eventframework.handler.timer.TimerHandler;
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

public class ClientTest {

    public ClientTest() {
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
    
    @Test
    public void echoTest() throws InterruptedException {
        Set<Handler> serverHandlers = new HashSet<>();
        serverHandlers.add(new LifecycleHandler());
        serverHandlers.add(new CommunicationHandler());
        BlockingClientResultListener serverResultListener =
                new BlockingClientResultListener();
        Processor serverProcessor = new EchoServerProcessor(1);
        Client serverClient = new Client(serverProcessor, serverResultListener,
                serverHandlers, Collections.<OutgoingSimplifier>emptySet(),
                Collections.<IncomingSimplifier>emptySet());
        serverClient.start();
        
        Thread.sleep(500L); // wait for server to set up
        
        Set<Handler> clientHandlers = new HashSet<>();
        clientHandlers.add(new LifecycleHandler());
        clientHandlers.add(new CommunicationHandler());
        BlockingClientResultListener clientResultListener =
                new BlockingClientResultListener();
        Processor processor = new EchoClientProcessor();
        Client clientClient = new Client(processor, clientResultListener,
                clientHandlers, Collections.<OutgoingSimplifier>emptySet(),
                Collections.<IncomingSimplifier>emptySet());
        clientClient.start();
        
        clientResultListener.waitForResult();
        serverResultListener.waitForResult();

        clientClient.stop();
        serverClient.stop();
    }
}
