package com.offbynull.actors.core.gateways.timer;

import com.offbynull.actors.core.gateways.direct.DirectGateway;
import com.offbynull.actors.core.shuttle.Message;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class TimerGatewayTest {

    private DirectGateway directGateway;
    private TimerGateway timerGateway;
    
    @Before
    public void before() {
        directGateway = DirectGateway.create("direct");
        timerGateway = TimerGateway.create("timer");

        directGateway.addOutgoingShuttle(timerGateway.getIncomingShuttle());
        timerGateway.addOutgoingShuttle(directGateway.getIncomingShuttle());
    }

    @After
    public void after() {
        IOUtils.closeQuietly(directGateway);
        IOUtils.closeQuietly(timerGateway);
    }

    @Test
    public void mustEchoBackMessageAfter200Milliseconds() throws Exception {
        directGateway.listen("direct:tester");
        
        directGateway.writeMessage("direct:tester:inner1:inner2", "timer:200:inner3:inner4", "payload");
        Message echoed = directGateway.readMessage("direct:tester", 200 + 100 /* 100ms of padding */, TimeUnit.MILLISECONDS);
        
        assertEquals("timer:200:inner3:inner4", echoed.getSourceAddress().toString());
        assertEquals("direct:tester:inner1:inner2", echoed.getDestinationAddress().toString());
        assertEquals("payload", echoed.getMessage());
    }
    
}
