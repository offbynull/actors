package com.offbynull.actors.gateways.threadpool;

import com.offbynull.actors.address.Address;
import com.offbynull.actors.gateways.direct.DirectGateway;
import com.offbynull.actors.shuttle.Message;
import com.offbynull.actors.shuttle.Shuttle;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ThreadPoolGatewayTest {
    private DirectGateway directGateway;
    private ThreadPoolGateway threadPoolGateway;
    
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    
    @Before
    public void before() {
        directGateway = DirectGateway.create("direct");
        Map<Class<?>, ThreadPoolProcessor> payloadTypes = new HashMap<>();
        payloadTypes.put(String.class, (message, outShuttles) -> {
            String fromPrefix = message.getSourceAddress().getElement(0);
            
            Shuttle outShuttle = outShuttles.get(fromPrefix);
            if (outShuttle != null) {
                Message outMessage = new Message(
                        message.getDestinationAddress(),
                        message.getSourceAddress(),
                        message.getMessage() + "RESPONSE");
                outShuttle.send(outMessage);
            }
        });
        threadPoolGateway = ThreadPoolGateway.create("threadpool", payloadTypes, 1, 10);

        directGateway.addOutgoingShuttle(threadPoolGateway.getIncomingShuttle());
        threadPoolGateway.addOutgoingShuttle(directGateway.getIncomingShuttle());
    }

    @After
    public void after() {
        IOUtils.closeQuietly(directGateway);
        IOUtils.closeQuietly(threadPoolGateway);
    }

    @Test(timeout = 2000L)
    public void mustListenOnRootAddresses() throws InterruptedException {
        directGateway.listen("direct:a");

        directGateway.writeMessage("direct:a", "threadpool:b", "test");
        Message msg = directGateway.readMessage("direct:a");

        assertEquals("testRESPONSE", msg.getMessage());
        assertEquals(Address.fromString("threadpool:b"), msg.getSourceAddress());
        assertEquals(Address.fromString("direct:a"), msg.getDestinationAddress());
    }
}
