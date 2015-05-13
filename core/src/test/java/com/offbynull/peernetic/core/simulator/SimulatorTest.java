package com.offbynull.peernetic.core.simulator;

import com.offbynull.peernetic.core.simulator.Simulator;
import com.offbynull.peernetic.core.simulator.ReplayMessageSource;
import com.offbynull.peernetic.core.simulator.MessageSink;
import com.offbynull.peernetic.core.simulator.RecordMessageSink;
import com.offbynull.peernetic.core.simulator.MessageSource;
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
import org.apache.commons.lang3.mutable.MutableBoolean;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SimulatorTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

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
                (src, dst, msg, realDuration) -> Duration.ofSeconds(1L));
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
                (src, dst, msg, realDuration) -> Duration.ofSeconds(1L));
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
                        Instant.ofEpochSecond(1L),
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
        fixture.addTimer("timer", Instant.ofEpochMilli(0L));
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
    public void mustShuttleMessagesBetweenActorAndTimerWithTimeOffsetAndActorDelay() {
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
                (src, dst, msg, realDuration) -> Duration.ofSeconds(1L));
        fixture.addTimer("timer", Instant.ofEpochMilli(0L));
        fixture.addCoroutineActor("local", tester, Duration.ofSeconds(1L), Instant.ofEpochMilli(0L), "timer");

        while (fixture.hasMore()) {
            fixture.process();
        }

        assertEquals(Arrays.asList("timer", 0), result);
        assertEquals(
                Arrays.asList(
                        Instant.ofEpochSecond(1L),
                        Instant.ofEpochSecond(4L)
                ),
                times);
    }

    @Test
    public void mustRecordAndReplayMessagesWithTimeOffsetAndActorDelay() throws Exception {
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
                        (src, dst, msg, realDuration) -> Duration.ofSeconds(2L));
                simulator.addTimer("timer", Instant.ofEpochSecond(10L));
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
                        (src, dst, msg, realDuration) -> Duration.ofSeconds(1L));
                simulator.addTimer("timer", Instant.ofEpochMilli(0L));
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
                            Instant.ofEpochSecond(5L)
                    ),
                    echoerTimes);
        }
    }

    @Test
    public void mustNotGetPendingMessagesComingInToActorThatWasRemovedAndReadded() {
        MutableBoolean failCalled = new MutableBoolean();
        Coroutine ignoreActor = (cnt) -> {
            while (true) {
                cnt.suspend();
            }
        };
        Coroutine failActor = (cnt) -> {
            failCalled.setTrue();
        };

        Simulator fixture = new Simulator(
                Instant.ofEpochMilli(0L),
                (src, dst, msg, realDuration) -> Duration.ofSeconds(5L));
        fixture.addCoroutineActor("local:test", ignoreActor, Duration.ZERO, Instant.ofEpochMilli(0L), "hi1", "hi2");
        fixture.removeActor("local:test", Instant.ofEpochMilli(1L));
        fixture.addCoroutineActor("local:test", failActor, Duration.ZERO, Instant.ofEpochMilli(2L));

        while (fixture.hasMore()) {
            fixture.process();
        }

        assertFalse(failCalled.booleanValue());
    }

    @Test
    public void mustNotSendTimerMessageIfTimerWasRemovedAndReadded() {
        MutableBoolean failCalled = new MutableBoolean();
        Coroutine triggerTimerActor = (cnt) -> {
            ((Context) cnt.getContext()).addOutgoingMessage("timer:5000", "failmsg");
            while (true) {
                cnt.suspend();
            }
        };
        Coroutine failActor = (cnt) -> {
            failCalled.setTrue();
        };

        Simulator fixture = new Simulator();
        fixture.addTimer("timer", Instant.ofEpochMilli(0L));
        fixture.addCoroutineActor("local:test", triggerTimerActor, Duration.ZERO, Instant.ofEpochMilli(0L), "sendmsg");

        fixture.removeActor("local:test", Instant.ofEpochMilli(1L));
        fixture.removeTimer("timer", Instant.ofEpochMilli(1L));

        fixture.addTimer("timer", Instant.ofEpochMilli(2L));
        fixture.addCoroutineActor("local:test", failActor, Duration.ZERO, Instant.ofEpochMilli(2L));

        while (fixture.hasMore()) {
            fixture.process();
        }

        assertFalse(failCalled.booleanValue());
    }

    @Test
    public void mustFailToRemoveUnknownTimer() {
        Simulator fixture = new Simulator();
        fixture.removeTimer("timer", Instant.ofEpochMilli(1L));

        exception.expect(IllegalArgumentException.class);
        while (fixture.hasMore()) {
            fixture.process();
        }
    }

    @Test
    public void mustFailToRemoveUnknownActor() {
        Simulator fixture = new Simulator();
        fixture.removeActor("actor", Instant.ofEpochMilli(1L));

        exception.expect(IllegalArgumentException.class);
        while (fixture.hasMore()) {
            fixture.process();
        }
    }

    @Test
    public void mustFailToRemoveActorThatHadAlreadyCompleted() {
        Coroutine actor = (cnt) -> {};
        
        Simulator fixture = new Simulator();
        fixture.addCoroutineActor("actor", actor, Duration.ZERO, Instant.EPOCH, "msg");
        fixture.removeActor("actor", Instant.ofEpochMilli(1L));

        exception.expect(IllegalArgumentException.class);
        while (fixture.hasMore()) {
            fixture.process();
        }
    }

    @Test
    public void mustAddAndRemoveActorWhenBothEventsOccurAtSameTime() {
        Coroutine actor = (cnt) -> {};
        
        Simulator fixture = new Simulator();
        fixture.addCoroutineActor("actor", actor, Duration.ZERO, Instant.EPOCH);
        fixture.removeActor("actor", Instant.EPOCH);

        while (fixture.hasMore()) {
            fixture.process();
        }
        
        // No exception means pass
    }

    @Test
    public void mustFailToRemoveAndAddActorWhenBothEventsOccurAtSameTime() {
        Coroutine actor = (cnt) -> {};
        
        // If happening at the same time, simulator runs in order events were added. So remove happens first then add happens, but remove
        // should throw an exception
        Simulator fixture = new Simulator();
        fixture.removeActor("actor", Instant.EPOCH);
        fixture.addCoroutineActor("actor", actor, Duration.ZERO, Instant.EPOCH);

        exception.expect(IllegalArgumentException.class);
        while (fixture.hasMore()) {
            fixture.process();
        }
    }
}
