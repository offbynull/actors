package com.offbynull.peernetic.eventframework.tests;

import com.offbynull.peernetic.eventframework.Client;
import com.offbynull.peernetic.eventframework.handler.Handler;
import com.offbynull.eventframework.network.impl.simpletcp.CommunicationHandler;
import com.offbynull.peernetic.eventframework.BlockingClientResultListener;
import com.offbynull.peernetic.eventframework.impl.basic.lifecycle.LifecycleHandler;
import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.simplifier.IncomingSimplifier;
import com.offbynull.peernetic.eventframework.simplifier.OutgoingSimplifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class EchoTest {

    public EchoTest() {
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
