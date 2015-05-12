package com.offbynull.peernetic.core.simulation;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SimulatorTest {

    @Test
    public void mustShuttleMessagesBetweenActors() {
        List<Integer> result = new ArrayList<>();

        Coroutine sender = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            String dstAddr = ctx.getIncomingMessage();

            for (int i = 0; i < 3; i++) {
                ctx.addOutgoingMessage(dstAddr, i);
                cnt.suspend();
                result.add((Integer) ctx.getIncomingMessage());
            }
        };

        Coroutine echoer = (cnt) -> {
            Context ctx = (Context) cnt.getContext();

            while (true) {
                String src = ctx.getSource();
                Object msg = ctx.getIncomingMessage();
                ctx.addOutgoingMessage(src, msg);
                cnt.suspend();
            }
        };

        Simulator fixture = new Simulator();
        fixture.addCoroutineActor("local:sender", sender, Duration.ZERO, Instant.ofEpochMilli(0L), "local:echoer");
        fixture.addCoroutineActor("local:echoer", echoer, Duration.ZERO, Instant.ofEpochMilli(0L));

        while (fixture.hasMore()) {
            fixture.process();
        }

        assertEquals(Arrays.asList(0, 1, 2), result);
    }

    @Test
    public void mustShuttleMessagesBetweenActorsWithTimeOffsets() {
        List<Integer> result = new ArrayList<>();
        List<Instant> senderTimes = new ArrayList<>();
        List<Instant> echoerTimes = new ArrayList<>();

        Coroutine sender = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            String dstAddr = ctx.getIncomingMessage();
            senderTimes.add(ctx.getTime());

            for (int i = 0; i < 3; i++) {
                ctx.addOutgoingMessage(dstAddr, i);
                cnt.suspend();
                result.add((Integer) ctx.getIncomingMessage());
                senderTimes.add(ctx.getTime());
            }
        };

        Coroutine echoer = (cnt) -> {
            Context ctx = (Context) cnt.getContext();

            while (true) {
                String src = ctx.getSource();
                Object msg = ctx.getIncomingMessage();
                echoerTimes.add(ctx.getTime());
                ctx.addOutgoingMessage(src, msg);
                cnt.suspend();
            }
        };

        Simulator fixture = new Simulator();
        fixture.addCoroutineActor("local:sender", sender, Duration.ofSeconds(1L), Instant.ofEpochMilli(0L), "local:echoer");
        fixture.addCoroutineActor("local:echoer", echoer, Duration.ofSeconds(2L), Instant.ofEpochMilli(0L));

        while (fixture.hasMore()) {
            fixture.process();
        }

        assertEquals(Arrays.asList(0, 1, 2), result);
        assertEquals(Collections.nCopies(4, Instant.ofEpochSecond(1L)), senderTimes);
        assertEquals(Collections.nCopies(3, Instant.ofEpochSecond(2L)), echoerTimes);
    }

    @Test
    public void mustShuttleMessagesBetweenActorsWithTimeOffsetsAndMessageDelays() {
        List<Integer> result = new ArrayList<>();
        List<Instant> senderTimes = new ArrayList<>();
        List<Instant> echoerTimes = new ArrayList<>();

        Coroutine sender = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            String dstAddr = ctx.getIncomingMessage();
            senderTimes.add(ctx.getTime());

            for (int i = 0; i < 3; i++) {
                ctx.addOutgoingMessage(dstAddr, i);
                cnt.suspend();
                result.add((Integer) ctx.getIncomingMessage());
                senderTimes.add(ctx.getTime());
            }
        };

        Coroutine echoer = (cnt) -> {
            Context ctx = (Context) cnt.getContext();

            while (true) {
                String src = ctx.getSource();
                Object msg = ctx.getIncomingMessage();
                echoerTimes.add(ctx.getTime());
                ctx.addOutgoingMessage(src, msg);
                cnt.suspend();
            }
        };

        Simulator fixture = new Simulator(
                Instant.ofEpochMilli(0L),
                new SimpleActorBehaviourDriver(),
                (src, dst, msg) -> Duration.ofSeconds(1L));
        fixture.addCoroutineActor("local:sender", sender, Duration.ofSeconds(1L), Instant.ofEpochMilli(0L), "local:echoer");
        fixture.addCoroutineActor("local:echoer", echoer, Duration.ofSeconds(2L), Instant.ofEpochMilli(0L));

        while (fixture.hasMore()) {
            fixture.process();
        }

        assertEquals(Arrays.asList(0, 1, 2), result);
        assertEquals(
                Arrays.asList(
                        Instant.ofEpochSecond(2L), // each msg takes 1sec to send, and 1sec to bounce back to sender... so 2sec in total
                        Instant.ofEpochSecond(4L),
                        Instant.ofEpochSecond(6L),
                        Instant.ofEpochSecond(8L)
                ),
                senderTimes);
        assertEquals(
                Arrays.asList(
                        Instant.ofEpochSecond(4L),
                        Instant.ofEpochSecond(6L),
                        Instant.ofEpochSecond(8L)
                ),
                echoerTimes);
    }

    @Test
    public void mustShuttleMessagesBetweenActorsWithTimeOffsetsWithActorDelays() {
        List<Integer> result = new ArrayList<>();
        List<Instant> senderTimes = new ArrayList<>();
        List<Instant> echoerTimes = new ArrayList<>();

        Coroutine sender = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            String dstAddr = ctx.getIncomingMessage();
            senderTimes.add(ctx.getTime());

            for (int i = 0; i < 3; i++) {
                ctx.addOutgoingMessage(dstAddr, i);
                cnt.suspend();
                result.add((Integer) ctx.getIncomingMessage());
                senderTimes.add(ctx.getTime());
            }
        };

        Coroutine echoer = (cnt) -> {
            Context ctx = (Context) cnt.getContext();

            while (true) {
                String src = ctx.getSource();
                Object msg = ctx.getIncomingMessage();
                echoerTimes.add(ctx.getTime());
                ctx.addOutgoingMessage(src, msg);
                cnt.suspend();
            }
        };

        Simulator fixture = new Simulator(
                Instant.ofEpochMilli(0L),
                (src, dst, msg, realDuration) -> Duration.ofSeconds(1L),
                new SimpleMessageBehaviourDriver());
        fixture.addCoroutineActor("local:sender", sender, Duration.ofSeconds(1L), Instant.ofEpochMilli(0L), "local:echoer");
        fixture.addCoroutineActor("local:echoer", echoer, Duration.ofSeconds(2L), Instant.ofEpochMilli(0L));

        while (fixture.hasMore()) {
            fixture.process();
        }

        assertEquals(Arrays.asList(0, 1, 2), result);
        assertEquals(
                Arrays.asList(
                        Instant.ofEpochSecond(1L), // each msg takes 1sec to process, initial priming msg comes in immediately w/o delay
                        Instant.ofEpochSecond(3L),
                        Instant.ofEpochSecond(5L),
                        Instant.ofEpochSecond(7L)
                ),
                senderTimes);
        assertEquals(
                Arrays.asList(
                        Instant.ofEpochSecond(3L),
                        Instant.ofEpochSecond(5L),
                        Instant.ofEpochSecond(7L)
                ),
                echoerTimes);
    }

    @Test
    public void mustShuttleMessagesBetweenActorsWithTimeOffsetsWithMessageDelaysAndActorDelays() {
        List<Integer> result = new ArrayList<>();
        List<Instant> senderTimes = new ArrayList<>();
        List<Instant> echoerTimes = new ArrayList<>();

        Coroutine sender = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            String dstAddr = ctx.getIncomingMessage();
            senderTimes.add(ctx.getTime());

            for (int i = 0; i < 3; i++) {
                ctx.addOutgoingMessage(dstAddr, i);
                cnt.suspend();
                result.add((Integer) ctx.getIncomingMessage());
                senderTimes.add(ctx.getTime());
            }
        };

        Coroutine echoer = (cnt) -> {
            Context ctx = (Context) cnt.getContext();

            while (true) {
                String src = ctx.getSource();
                Object msg = ctx.getIncomingMessage();
                echoerTimes.add(ctx.getTime());
                ctx.addOutgoingMessage(src, msg);
                cnt.suspend();
            }
        };

        Simulator fixture = new Simulator(
                Instant.ofEpochMilli(0L),
                (src, dst, msg, realDuration) -> Duration.ofSeconds(1L),
                (src, dst, msg) -> Duration.ofSeconds(1L));
        fixture.addCoroutineActor("local:sender", sender, Duration.ofSeconds(1L), Instant.ofEpochMilli(0L), "local:echoer");
        fixture.addCoroutineActor("local:echoer", echoer, Duration.ofSeconds(2L), Instant.ofEpochMilli(0L));

        while (fixture.hasMore()) {
            fixture.process();
        }

        assertEquals(Arrays.asList(0, 1, 2), result);
        assertEquals(
                Arrays.asList(
                        // each msg takes 1sec to process, initial priming msg comes in immediately w/o delay
                          // also, each msg takes 1sec to send, and 1sec to bounce back to sender... so 2sec in total
                          // don't forget priming msg also has a 1sec delay before arriving
                        Instant.ofEpochSecond(2L),
                        Instant.ofEpochSecond(6L),
                        Instant.ofEpochSecond(10L),
                        Instant.ofEpochSecond(14L)
                ),
                senderTimes);
        assertEquals(
                Arrays.asList(
                        Instant.ofEpochSecond(5L),
                        Instant.ofEpochSecond(9L),
                        Instant.ofEpochSecond(13L)
                ),
                echoerTimes);
    }

}
