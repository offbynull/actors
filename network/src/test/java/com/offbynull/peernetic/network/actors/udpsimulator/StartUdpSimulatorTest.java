package com.offbynull.peernetic.network.actors.udpsimulator;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.simulator.Simulator;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class StartUdpSimulatorTest {

    @Test
    public void mustProperlySimulateUdp() throws Exception {

        List<Object> queue = new LinkedList<>();

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

        Simulator simulator = new Simulator();
        simulator.addTimer("timer", Instant.ofEpochMilli(0L));
        simulator.addActor("echoer", echoer, Duration.ZERO, Instant.ofEpochMilli(0L));
        simulator.addActor("echoerproxy", new UdpSimulatorCoroutine(), Duration.ZERO, Instant.ofEpochMilli(0L),
                new StartUdpSimulator(
                        Address.of("timer"),
                        Address.fromString("echoer"),
                        () -> new SimpleLine(
                                0L,
                                Duration.ofSeconds(1L),
                                Duration.ofSeconds(1L),
                                0.1,
                                0.1,
                                10,
                                1500,
                                new SimpleSerializer())));
        simulator.addActor("sender", sender, Duration.ZERO, Instant.ofEpochMilli(0L),
                Address.fromString("senderproxy:echoerproxy"));
        simulator.addActor("senderproxy", new UdpSimulatorCoroutine(), Duration.ZERO, Instant.ofEpochMilli(0L),
                new StartUdpSimulator(
                        Address.of("timer"),
                        Address.fromString("sender"),
                        () -> new SimpleLine(
                                0L,
                                Duration.ofSeconds(1L),
                                Duration.ofSeconds(1L),
                                0.1,
                                0.1,
                                10,
                                1500,
                                new SimpleSerializer())));

        while (simulator.hasMore()) {
            simulator.process();
        }

        assertEquals(Arrays.asList(0, 2, 2, 6, 9, 3, 7, 9, 8, 5, 6, 4), queue);
    }

}
