package com.offbynull.peernetic.core.simulation;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.common.SimpleSerializer;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.Validate;
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
    public void mustShuttleMessagesBetweenActorsWithTimeOffsetsAndActorDelays() {
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

    @Test
    public void mustShuttleMessagesBetweenActorAndTimer() {
        List<Object> result = new ArrayList<>();
        List<Instant> times = new ArrayList<>();
        Coroutine tester = (cnt) -> {
            Context ctx = (Context) cnt.getContext();

            result.add(ctx.getIncomingMessage());
            times.add(ctx.getTime());

            String timerPrefix = ctx.getIncomingMessage();
            ctx.addOutgoingMessage(timerPrefix + ":2000", 0);
            cnt.suspend();

            result.add(ctx.getIncomingMessage());
            times.add(ctx.getTime());
        };

        Simulator fixture = new Simulator();
        fixture.addTimer("timer", 0L, Instant.ofEpochMilli(0L));
        fixture.addCoroutineActor("local", tester, Duration.ZERO, Instant.ofEpochMilli(0L), "timer");

        while (fixture.hasMore()) {
            fixture.process();
        }

        assertEquals(Arrays.asList("timer", 0), result);
        assertEquals(
                Arrays.asList(
                        Instant.ofEpochSecond(0L),
                        Instant.ofEpochSecond(2L)
                ),
                times);
    }

    @Test
    public void mustShuttleMessagesBetweenActorAndTimerWithTimeOffsetAndMessageDelayAndActorDelay() {
        List<Object> result = new ArrayList<>();
        List<Instant> times = new ArrayList<>();
        Coroutine tester = (cnt) -> {
            Context ctx = (Context) cnt.getContext();

            result.add(ctx.getIncomingMessage());
            times.add(ctx.getTime());

            String timerPrefix = ctx.getIncomingMessage();
            ctx.addOutgoingMessage(timerPrefix + ":2000", 0);
            cnt.suspend();

            result.add(ctx.getIncomingMessage());
            times.add(ctx.getTime());
        };

        Simulator fixture = new Simulator(
                Instant.ofEpochMilli(0L),
                (src, dst, msg, realDuration) -> Duration.ofSeconds(1L),
                (src, dst, msg) -> Duration.ofSeconds(1L));
        fixture.addTimer("timer", 0L, Instant.ofEpochMilli(0L));
        fixture.addCoroutineActor("local", tester, Duration.ofSeconds(1L), Instant.ofEpochMilli(0L), "timer");

        while (fixture.hasMore()) {
            fixture.process();
        }

        assertEquals(Arrays.asList("timer", 0), result);
        assertEquals(
                Arrays.asList(
                        Instant.ofEpochSecond(2L),
                        Instant.ofEpochSecond(7L)
                ),
                times);
    }

    @Test
    public void mustRecordAndReplayMessagesWithTimeOffsetAndMessageDelayAndActorDelay() throws Exception {
        File recordFile = File.createTempFile(getClass().getSimpleName(), ".data");
        recordFile.deleteOnExit();

        // recording echo
        {
            Coroutine sender = (cnt) -> {
                Context ctx = (Context) cnt.getContext();
                String dstAddr = ctx.getIncomingMessage();

                for (int i = 0; i < 3; i++) {
                    ctx.addOutgoingMessage(dstAddr, i);
                    cnt.suspend();
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

            try (MessageSink sink = new RecordMessageSink("local:echoer", recordFile, new SimpleSerializer())) {
                Simulator simulator = new Simulator(
                        Instant.ofEpochSecond(10L),
                        (src, dst, msg, realDuration) -> Duration.ofSeconds(2L),
                        (src, dst, msg) -> Duration.ofSeconds(1L));
                simulator.addTimer("timer", 0L, Instant.ofEpochSecond(10L));
                simulator.addMessageSink(sink, Instant.ofEpochSecond(10L));
                simulator.addCoroutineActor("local:sender", sender, Duration.ofSeconds(5L), Instant.ofEpochSecond(10L), "local:echoer");
                simulator.addCoroutineActor("local:echoer", echoer, Duration.ofSeconds(10L), Instant.ofEpochSecond(10L));

                while (simulator.hasMore()) {
                    simulator.process();
                }
            }
        }

        // replaying recording of echo
        {
            List<Integer> result = new ArrayList<>();
            List<Instant> echoerTimes = new ArrayList<>();

            Coroutine echoer = (cnt) -> {
                Context ctx = (Context) cnt.getContext();

                while (true) {
                    String src = ctx.getSource();
                    Object msg = ctx.getIncomingMessage();
                    ctx.addOutgoingMessage(src, msg);
                    result.add((Integer) ctx.getIncomingMessage());
                    echoerTimes.add(ctx.getTime());
                    cnt.suspend();
                }
            };

            try (MessageSource source = new ReplayMessageSource("local:echoer", recordFile, new SimpleSerializer())) {
                Simulator simulator = new Simulator(
                        Instant.ofEpochMilli(0L),
                        (src, dst, msg, realDuration) -> Duration.ofSeconds(1L),
                        (src, dst, msg) -> Duration.ofSeconds(2L));
                simulator.addTimer("timer", 0L, Instant.ofEpochMilli(0L));
                simulator.addCoroutineActor("local:echoer", echoer, Duration.ofSeconds(1L), Instant.ofEpochMilli(0L));
                simulator.addMessageSource(source, Instant.ofEpochMilli(0L)); // add the msg source after the priming msg

                while (simulator.hasMore()) {
                    simulator.process();
                }
            }

            // Note that this simulator was just replaying events with the durations it reads out. If the actor that processes these
            // replayed messages delays (as this simulator was set to do), or the messages coming from the source delay (as this test
            // simulator was also set to do), the simulator should properly account for this (the assertions below checks that it does)
            assertEquals(Arrays.asList(0, 1, 2), result);
            assertEquals(
                    Arrays.asList(
                            Instant.ofEpochSecond(1L),
                            Instant.ofEpochSecond(2L),
                            Instant.ofEpochSecond(7L)
                    ),
                    echoerTimes);
        }
    }
    
    // TODO: Add test: 1 sends a message to 2. The message takes 5 seconds to arrive. 1 drops at 2seconds, 2 drops at 3seconds, 2 is readded at 4seconds. 2 should still get the message at at 5 seconds.
    // TODO: Add test: Similar to above but with timer
}
