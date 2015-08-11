package com.offbynull.peernetic.network.gateways.udp;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.ActorRunner;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;
import static org.junit.Assert.assertEquals;
import org.junit.Ignore;
import org.junit.Test;

public class UdpGatewayTest {

    @Test
    @Ignore(value = "UDP is not deterministic. This test expects all packets will arrive, which may not happen.")
    public void mustProperlySendAndReceiveMessagesOverUdp() throws Exception {
        // This test expects 0 packet loss/duplication. Packet order, however, does not matter.
        ActorRunner echoerRunner = null;
        UdpGateway echoerUdpGateway = null;
        ActorRunner senderRunner = null;
        UdpGateway senderUdpGateway = null;
        
        try {
            LinkedBlockingQueue<Object> queue = new LinkedBlockingQueue<>();

            Coroutine sender = (cnt) -> {
                Context ctx = (Context) cnt.getContext();
                Address dstAddr = ctx.getIncomingMessage();

                for (int i = 0; i < 10; i++) {
                    ctx.addOutgoingMessage(Address.of("hi"), dstAddr, i);
                }

                while (true) {
                    cnt.suspend();
                    queue.add(ctx.getIncomingMessage());
                }
            };

            Coroutine echoer = (cnt) -> {
                Context ctx = (Context) cnt.getContext();

                while (true) {
                    Address src = ctx.getSource();
                    Object msg = ctx.getIncomingMessage();
                    ctx.addOutgoingMessage(src, msg);
                    cnt.suspend();
                }
            };

            echoerRunner = new ActorRunner("echoer");
            Shuttle echoerInputShuttle = echoerRunner.getIncomingShuttle();
            echoerUdpGateway = new UdpGateway(
                    new InetSocketAddress(1000),
                    "internaludp",
                    echoerInputShuttle,
                    Address.fromString("echoer:echoer"),
                    new SimpleSerializer());
            Shuttle echoerUdpOutputShuttle = echoerUdpGateway.getIncomingShuttle();
            echoerRunner.addOutgoingShuttle(echoerUdpOutputShuttle);

            senderRunner = new ActorRunner("sender");
            Shuttle senderInputShuttle = senderRunner.getIncomingShuttle();
            senderUdpGateway = new UdpGateway(
                    new InetSocketAddress(2000),
                    "internaludp",
                    senderInputShuttle,
                    Address.fromString("sender:sender"),
                    new SimpleSerializer());
            Shuttle senderUdpOutputShuttle = senderUdpGateway.getIncomingShuttle();
            senderRunner.addOutgoingShuttle(senderUdpOutputShuttle);

            echoerRunner.addCoroutineActor("echoer", echoer);
            senderRunner.addCoroutineActor("sender", sender, Address.fromString("internaludp:7f000001.1000"));

            while (true) {
                if (queue.size() >= 10) {
                    break;
                }
                Thread.sleep(1000L);
            }
            
            HashSet<Object> expected = new HashSet<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
            HashSet<Object> actual = new HashSet<>();
            queue.drainTo(actual);
            
            assertEquals(expected, actual);
        } finally {
            if (senderRunner != null) {
                try {
                    senderRunner.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
            if (echoerRunner != null) {
                try {
                    echoerRunner.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
            if (senderUdpGateway != null) {
                try {
                    senderUdpGateway.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
            if (echoerUdpGateway != null) {
                try {
                    echoerUdpGateway.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
    }

}
