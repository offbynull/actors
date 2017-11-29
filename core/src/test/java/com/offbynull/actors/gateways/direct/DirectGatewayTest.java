package com.offbynull.actors.gateways.direct;

import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DirectGatewayTest {

    private DirectGateway direct1Gateway;
    private DirectGateway direct2Gateway;
    
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    
    @Before
    public void before() {
        direct1Gateway = DirectGateway.create("direct1");
        direct2Gateway = DirectGateway.create("direct2");

        direct1Gateway.addOutgoingShuttle(direct2Gateway.getIncomingShuttle());
        direct2Gateway.addOutgoingShuttle(direct1Gateway.getIncomingShuttle());
    }
    
    @After
    public void after() {
        IOUtils.closeQuietly(direct1Gateway);
        IOUtils.closeQuietly(direct2Gateway);
    }

    @Test
    public void mustFailToListenOnAddressWithBadPrefix() throws InterruptedException {
        thrown.expect(IllegalArgumentException.class);
        direct1Gateway.listen("xxxx:in");
    }

    @Test
    public void mustFailToReadFromAddressBelowListenAddress() throws InterruptedException {
        direct1Gateway.listen("direct1:in1:in2");
        thrown.expect(IllegalArgumentException.class);
        direct1Gateway.readMessagePayloadOnly("direct1:in1", 100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void mustFailToUnlistenOnAddressWithBadPrefix() throws InterruptedException {
        thrown.expect(IllegalArgumentException.class);
        direct1Gateway.unlisten("xxxx:in");
    }

    @Test
    public void mustFailToWriteWithBadSourcePrefix() throws InterruptedException {
        thrown.expect(IllegalArgumentException.class);
        direct1Gateway.writeMessage("xxxx:in", "direct2", "payload");
    }

    @Test
    public void mustNotFailWhenUnlisteningAddressThatWasNeverListenedTo() throws InterruptedException {
        direct1Gateway.unlisten("direct1:never_listened_to");
    }
    
    @Test
    public void mustListenOnRootAddresses() throws InterruptedException {
        direct1Gateway.listen("direct1");
        direct2Gateway.listen("direct2");

        direct1Gateway.writeMessage("direct1", "direct2", "1to2");
        direct2Gateway.writeMessage("direct2", "direct1", "2to1");

        assertEquals("2to1", direct1Gateway.readMessagePayloadOnly("direct1"));
        assertEquals("1to2", direct2Gateway.readMessagePayloadOnly("direct2"));
    }

    @Test
    public void mustListenBelowRootAddresses() throws InterruptedException {
        direct1Gateway.listen("direct1:in");
        direct2Gateway.listen("direct2:in");

        direct1Gateway.writeMessage("direct1:in", "direct2:in", "1to2");
        direct2Gateway.writeMessage("direct2:in", "direct1:in", "2to1");

        assertEquals("2to1", direct1Gateway.readMessagePayloadOnly("direct1:in"));
        assertEquals("1to2", direct2Gateway.readMessagePayloadOnly("direct2:in"));
    }

    @Test
    public void mustListenToBothRootAddressAndAddressBelowRoot() throws InterruptedException {
        direct1Gateway.listen("direct1");
        direct1Gateway.listen("direct1:in");
        direct2Gateway.listen("direct2:in");

        direct1Gateway.writeMessage("direct1:in", "direct2:in", "1to2");
        direct2Gateway.writeMessage("direct2:in", "direct1:in", "2to1");

        assertEquals("2to1", direct1Gateway.readMessagePayloadOnly("direct1"));
        assertEquals("2to1", direct1Gateway.readMessagePayloadOnly("direct1:in"));
        assertEquals("1to2", direct2Gateway.readMessagePayloadOnly("direct2:in"));
    }

    @Test
    public void mustUnlistenToAddressThatWasBeingListenedOn() throws InterruptedException {
        direct1Gateway.listen("direct1:in");
        direct2Gateway.listen("direct2:in");

        direct1Gateway.writeMessage("direct1:in", "direct2:in", "1to2");
        direct2Gateway.writeMessage("direct2:in", "direct1:in", "2to1");

        assertEquals("2to1", direct1Gateway.readMessagePayloadOnly("direct1:in"));
        assertEquals("1to2", direct2Gateway.readMessagePayloadOnly("direct2:in"));
        
        direct2Gateway.unlisten("direct2:in");
        
        direct1Gateway.writeMessage("direct1:in", "direct2:in", "fake");
        thrown.expect(IllegalArgumentException.class);
        direct2Gateway.readMessagePayloadOnly("direct2:in");
    }
}
