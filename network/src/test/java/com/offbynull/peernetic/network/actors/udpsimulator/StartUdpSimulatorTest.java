package com.offbynull.peernetic.network.actors.udpsimulator;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.gateways.timer.TimerGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class StartUdpSimulatorTest {

    @Test
    public void mustProperlySimulateUdp() throws Exception {
        TimerGateway timerGateway = null;
        ActorThread echoerThread = null;
        ActorThread senderThread = null;
        
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



            timerGateway = new TimerGateway("timer");
            echoerThread = ActorThread.create("echoer");
            senderThread = ActorThread.create("sender");

            echoerThread.addOutgoingShuttle(senderThread.getIncomingShuttle());
            echoerThread.addOutgoingShuttle(timerGateway.getIncomingShuttle());

            senderThread.addOutgoingShuttle(echoerThread.getIncomingShuttle());
            senderThread.addOutgoingShuttle(timerGateway.getIncomingShuttle());

            timerGateway.addOutgoingShuttle(senderThread.getIncomingShuttle());
            timerGateway.addOutgoingShuttle(echoerThread.getIncomingShuttle());

            echoerThread.addCoroutineActor("echoer", echoer);
            echoerThread.addCoroutineActor("proxy", new UdpSimulatorCoroutine(),
                    new StartUdpSimulator(
                            Address.of("timer"),
                            Address.fromString("echoer:echoer"),
                            () -> new SimpleLine(
                                    0L,
                                    Duration.ofSeconds(1L),
                                    Duration.ofSeconds(1L),
                                    0.1,
                                    0.1,
                                    10,
                                    1500,
                                    new SimpleSerializer())));

            senderThread.addCoroutineActor("sender", sender, Address.fromString("sender:proxy:echoer:proxy"));
            senderThread.addCoroutineActor("proxy", new UdpSimulatorCoroutine(),
                    new StartUdpSimulator(
                            Address.of("timer"),
                            Address.fromString("sender:sender"),
                            () -> new SimpleLine(
                                    0L,
                                    Duration.ofSeconds(1L),
                                    Duration.ofSeconds(1L),
                                    0.1,
                                    0.1,
                                    10,
                                    1500,
                                    new SimpleSerializer())));
            
            assertEquals(0, queue.take());
            assertEquals(2, queue.take());
            assertEquals(2, queue.take());
            assertEquals(6, queue.take());
            assertEquals(9, queue.take());
            assertEquals(3, queue.take());
            assertEquals(7, queue.take());
            assertEquals(9, queue.take());
            assertEquals(8, queue.take());
            assertEquals(5, queue.take());
            assertEquals(6, queue.take());
            assertEquals(4, queue.take());
        } finally {
            if (senderThread != null) {
                try {
                    senderThread.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
            if (echoerThread != null) {
                try {
                    echoerThread.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
            if (timerGateway != null) {
                try {
                    timerGateway.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
    }
    
}
