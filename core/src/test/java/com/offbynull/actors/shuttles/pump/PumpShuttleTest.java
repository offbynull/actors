package com.offbynull.actors.shuttles.pump;

import com.offbynull.actors.gateways.direct.DirectGateway;
import com.offbynull.actors.shuttle.Message;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

public class PumpShuttleTest {

    private PumpShuttleController pumpShuttleController;
    private DirectGateway directGateway;
    
    @Before
    public void setUp() {
        directGateway = DirectGateway.create();
        pumpShuttleController = PumpShuttle.create(directGateway.getIncomingShuttle());
    }
    
    @After
    public void tearDown() throws InterruptedException {
        pumpShuttleController.close();
        pumpShuttleController.join();
        directGateway.close();
    }

    @Test
    public void mustPumpMessagesToGateway() throws InterruptedException {
        List<Message> messages = Arrays.asList(
                new Message("direct:src:1", "direct:dst:1", "payload1"),
                new Message("direct:src:2", "direct:dst:2", "payload2"),
                new Message("direct:src:3", "direct:dst:3", "payload3")
        );

        directGateway.listen("direct:dst");
        pumpShuttleController.getPumpShuttle().send(messages);
        
        Message message;
        
        message = directGateway.readMessage("direct:dst");
        assertEquals(messages.get(0), message);
        message = directGateway.readMessage("direct:dst");
        assertEquals(messages.get(1), message);
        message = directGateway.readMessage("direct:dst");
        assertEquals(messages.get(2), message);
    }

    @Test
    public void mustNotPumpMessagesIfClosed() throws InterruptedException {
        List<Message> messages = Arrays.asList(
                new Message("direct:src:1", "direct:dst:1", "payload1"),
                new Message("direct:src:2", "direct:dst:2", "payload2"),
                new Message("direct:src:3", "direct:dst:3", "payload3")
        );

        directGateway.listen("direct:dst");
        pumpShuttleController.close();
        pumpShuttleController.join();
        pumpShuttleController.getPumpShuttle().send(messages);
        
        Message message;
        message = directGateway.readMessage("direct:dst", 300L, TimeUnit.MILLISECONDS);
        assertNull(message);
    }
    
}
