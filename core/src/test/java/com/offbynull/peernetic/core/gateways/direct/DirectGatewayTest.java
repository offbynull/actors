package com.offbynull.peernetic.core.gateways.direct;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.ActorRunner;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.gateways.log.LogGateway;
import com.offbynull.peernetic.core.gateways.log.LogMessage;
import com.offbynull.peernetic.core.shuttle.Address;
import org.junit.Assert;
import org.junit.Test;

public class DirectGatewayTest {

    @Test
    public void testSomeMethod() throws InterruptedException {
        Coroutine echoerActor = (cnt) -> {
            final Address loggerAddress = Address.of("log");
            final Address directAddress = Address.of("direct");

            Context ctx = (Context) cnt.getContext();

            do {
                Object value = ctx.getIncomingMessage();
                ctx.addOutgoingMessage(loggerAddress, LogMessage.debug("Received an echo: {}", value));
                ctx.addOutgoingMessage(directAddress, value);
                
                if (value.equals("quit")) {
                    return;
                }
                
                cnt.suspend();
            } while (true);
        };

        ActorRunner actorRunner = new ActorRunner("actors");
        LogGateway logGateway = new LogGateway("log");
        DirectGateway directGateway = new DirectGateway("direct");

        directGateway.addOutgoingShuttle(actorRunner.getIncomingShuttle());
        actorRunner.addOutgoingShuttle(directGateway.getIncomingShuttle());
        actorRunner.addOutgoingShuttle(logGateway.getIncomingShuttle());

        actorRunner.addCoroutineActor("echoer", echoerActor);

        
        Address echoerAddress = Address.of("actors", "echoer");
        
        String expected;
        String actual;
        
        expected = "test1";
        directGateway.writeMessage(echoerAddress, expected);
        actual = (String) directGateway.readMessages().get(0).getMessage();
        Assert.assertEquals(expected, actual);

        expected = "test2";
        directGateway.writeMessage(echoerAddress, expected);
        actual = (String) directGateway.readMessages().get(0).getMessage();
        Assert.assertEquals(expected, actual);
        
        expected = "quit";
        directGateway.writeMessage(echoerAddress, expected);
        actual = (String) directGateway.readMessages().get(0).getMessage();
        Assert.assertEquals(expected, actual);
        
        actorRunner.close();
        logGateway.close();
        directGateway.close();
    }

}
