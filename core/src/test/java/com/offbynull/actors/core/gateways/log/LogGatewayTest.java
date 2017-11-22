package com.offbynull.actors.core.gateways.log;

import com.offbynull.actors.core.gateways.direct.DirectGateway;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LogGatewayTest {

    private DirectGateway directGateway;
    private LogGateway logGateway;
    
    @Before
    public void before() {
        directGateway = DirectGateway.create("direct");
        logGateway = LogGateway.create("log");

        directGateway.addOutgoingShuttle(logGateway.getIncomingShuttle());
        logGateway.addOutgoingShuttle(directGateway.getIncomingShuttle());
    }
    
    @After
    public void after() {
        IOUtils.closeQuietly(directGateway);
        IOUtils.closeQuietly(logGateway);
    }

    // This is difficult to test because its piping to SLF4J, but we can atleast add in a sanity test here
    @Test
    public void mustNotCrash() throws InterruptedException {
        // Remember that since these gateways running in different threads, tehy can close before the log gateway gets all 4 messages
        directGateway.writeMessage("direct", "log", LogMessage.error("log msg!!!! {}", 1));
        directGateway.writeMessage("direct", "log", LogMessage.error("log msg!!!! {}", 2));
        directGateway.writeMessage("direct", "log", LogMessage.error("log msg!!!! {}", 3));
        directGateway.writeMessage("direct", "log", LogMessage.error("log msg!!!! {}", 4));
    }
}
