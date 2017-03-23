package com.offbynull.peernetic.core.simulator;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.Address;
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
            ctx.allow();
            
            cnt.suspend();
            
            Address dstAddr = ctx.in();

            for (int i = 0; i < 3; i++) {
                ctx.out(dstAddr, i);
                cnt.suspend();
                result.add((Integer) ctx.in());
            }
        };

        Coroutine echoer = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();

            cnt.suspend();
            
            while (true) {
                Address src = ctx.source();
                Object msg = ctx.in();
                ctx.out(src, msg);
                cnt.suspend();
            }
        };

        Simulator fixture = new Simulator();
        fixture.addActor("echoer", echoer, Duration.ZERO, Instant.ofEpochMilli(0L), new Object());
        fixture.addActor("sender", sender, Duration.ZERO, Instant.ofEpochMilli(0L), new Object(), Address.fromString("echoer"));

        while (fixture.hasMore()) {
            fixture.process();
        }

        assertEquals(Arrays.asList(0, 1, 2), result);
    }

    @Test
    public void mustShuttleBatchedMessagesBetweenActors() {
        List<Integer> result = new ArrayList<>();

        Coroutine sender = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            
            cnt.suspend();
            
            Address dstAddr = ctx.in();

            ctx.out(dstAddr, 0);
            ctx.out(dstAddr, 1);
            ctx.out(dstAddr, 2);
            ctx.out("timer:2000", new Object());
            ctx.out(dstAddr, 100);
            ctx.out(dstAddr, 101);
            ctx.out(dstAddr, 102);

            for (int i = 0; i < 3; i++) {
                cnt.suspend();
                result.add((Integer) ctx.in());
            }
            
            for (int i = 0; i < 3; i++) {
                cnt.suspend();
                result.add((Integer) ctx.in());
            }
            
            cnt.suspend(); // wait for timer msg in 2 scs before stoping
        };

        Coroutine echoer = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            
            cnt.suspend();

            while (true) {
                Address src = ctx.source();
                Object msg = ctx.in();
                ctx.out(src, msg);
                cnt.suspend();
            }
        };

        Simulator fixture = new Simulator();
        fixture.addActor("echoer", echoer, Duration.ZERO, Instant.ofEpochMilli(0L), new Object());
        fixture.addActor("sender", sender, Duration.ZERO, Instant.ofEpochMilli(0L), new Object(), Address.fromString("echoer"));
        fixture.addTimer("timer", Instant.ofEpochMilli(0L));

        while (fixture.hasMore()) {
            fixture.process();
        }

        assertEquals(Arrays.asList(0, 1, 2, 100, 101, 102), result);
    }

    @Test
    public void mustShuttleMessagesBetweenActorsWithTimeOffsets() {
        List<Integer> result = new ArrayList<>();
        List<Instant> senderTimes = new ArrayList<>();
        List<Instant> echoerTimes = new ArrayList<>();

        Coroutine sender = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            
            cnt.suspend();
            
            Address dstAddr = ctx.in();
            senderTimes.add(ctx.time());

            for (int i = 0; i < 3; i++) {
                ctx.out(dstAddr, i);
                cnt.suspend();
                result.add((Integer) ctx.in());
                senderTimes.add(ctx.time());
            }
        };

        Coroutine echoer = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();

            cnt.suspend();
            
            while (true) {
                Address src = ctx.source();
                Object msg = ctx.in();
                echoerTimes.add(ctx.time());
                ctx.out(src, msg);
                cnt.suspend();
            }
        };

        Simulator fixture = new Simulator();
        fixture.addActor("echoer", echoer, Duration.ofSeconds(2L), Instant.ofEpochMilli(0L), new Object());
        fixture.addActor("sender", sender, Duration.ofSeconds(1L), Instant.ofEpochMilli(0L), new Object(), Address.of("echoer"));

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
            ctx.allow();
            
            cnt.suspend();
            
            Address dstAddr = ctx.in();
            senderTimes.add(ctx.time());

            for (int i = 0; i < 3; i++) {
                ctx.out(dstAddr, i);
                cnt.suspend();
                result.add((Integer) ctx.in());
                senderTimes.add(ctx.time());
            }
        };

        Coroutine echoer = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();

            cnt.suspend();
            
            while (true) {
                Address src = ctx.source();
                Object msg = ctx.in();
                echoerTimes.add(ctx.time());
                ctx.out(src, msg);
                cnt.suspend();
            }
        };

        Simulator fixture = new Simulator(
                Instant.ofEpochMilli(0L),
                (src, dst, msg, realDuration) -> Duration.ofSeconds(1L));
        fixture.addActor("echoer", echoer, Duration.ofSeconds(1L), Instant.ofEpochMilli(0L), new Object());
        fixture.addActor("sender", sender, Duration.ofSeconds(2L), Instant.ofEpochMilli(0L), new Object(), Address.of("echoer"));

        while (fixture.hasMore()) {
            fixture.process();
        }

        assertEquals(Arrays.asList(0, 1, 2), result);
        assertEquals(
                Arrays.asList(
                        Instant.ofEpochSecond(3L), // each msg takes 1sec to process, initial priming msg comes in immediately w/o delay
                        Instant.ofEpochSecond(5L),
                        Instant.ofEpochSecond(7L),
                        Instant.ofEpochSecond(9L)
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
            ctx.allow();
            
            cnt.suspend();
            
            Address dstAddr = ctx.in();
            senderTimes.add(ctx.time());

            for (int i = 0; i < 3; i++) {
                ctx.out(dstAddr, i);
                cnt.suspend();
                result.add((Integer) ctx.in());
                senderTimes.add(ctx.time());
            }
        };

        Coroutine echoer = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();

            cnt.suspend();
            
            while (true) {
                Address src = ctx.source();
                Object msg = ctx.in();
                echoerTimes.add(ctx.time());
                ctx.out(src, msg);
                cnt.suspend();
            }
        };

        Simulator fixture = new Simulator(
                Instant.ofEpochMilli(0L),
                (src, dst, msg, realDuration) -> Duration.ofSeconds(1L));
        fixture.addActor("echoer", echoer, Duration.ofSeconds(1L), Instant.ofEpochMilli(0L), new Object());
        fixture.addActor("sender", sender, Duration.ofSeconds(2L), Instant.ofEpochMilli(0L), new Object(), Address.of("echoer"));

        while (fixture.hasMore()) {
            fixture.process();
        }

        assertEquals(Arrays.asList(0, 1, 2), result);
        assertEquals(
                Arrays.asList(
                        // each msg takes 1sec to process, initial priming msg comes in immediately w/o delay
                        // also, each msg takes 1sec to send, and 1sec to bounce back to sender... so 2sec in total
                        // don't forget priming msg also has a 1sec delay before arriving
                        Instant.ofEpochSecond(3L),
                        Instant.ofEpochSecond(5L),
                        Instant.ofEpochSecond(7L),
                        Instant.ofEpochSecond(9L)
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
            ctx.allow();

            result.add(ctx.in());
            times.add(ctx.time());

            Address timerPrefix = ctx.in();
            ctx.out(timerPrefix.appendSuffix("2000"), 0);
            cnt.suspend();

            result.add(ctx.in());
            times.add(ctx.time());
        };

        Simulator fixture = new Simulator();
        fixture.addTimer("timer", Instant.ofEpochMilli(0L));
        fixture.addActor("local", tester, Duration.ZERO, Instant.ofEpochMilli(0L), Address.of("timer"));

        while (fixture.hasMore()) {
            fixture.process();
        }

        assertEquals(Arrays.asList(Address.of("timer"), 0), result);
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
            ctx.allow();

            result.add(ctx.in());
            times.add(ctx.time());

            Address timerPrefix = ctx.in();
            ctx.out(timerPrefix.appendSuffix("2000"), 0);
            cnt.suspend();

            result.add(ctx.in());
            times.add(ctx.time());
        };

        Simulator fixture = new Simulator(
                Instant.ofEpochMilli(0L),
                (src, dst, msg, realDuration) -> Duration.ofSeconds(1L));
        fixture.addTimer("timer", Instant.ofEpochMilli(0L));
        fixture.addActor("local", tester, Duration.ofSeconds(1L), Instant.ofEpochMilli(0L), Address.of("timer"));

        while (fixture.hasMore()) {
            fixture.process();
        }

        assertEquals(Arrays.asList(Address.of("timer"), 0), result);
        assertEquals(
                Arrays.asList(
                        Instant.ofEpochSecond(1L),
                        Instant.ofEpochSecond(4L)
                ),
                times);
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
        fixture.addActor("test", ignoreActor, Duration.ZERO, Instant.ofEpochMilli(0L), "hi1", "hi2");
        fixture.removeActor("test", Instant.ofEpochMilli(1L));
        fixture.addActor("test", failActor, Duration.ZERO, Instant.ofEpochMilli(2L));

        while (fixture.hasMore()) {
            fixture.process();
        }

        assertFalse(failCalled.booleanValue());
    }

    @Test
    public void mustNotSendTimerMessageIfTimerWasRemovedAndReadded() {
        MutableBoolean failCalled = new MutableBoolean();
        Coroutine triggerTimerActor = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            ctx.out("timer:5000", "failmsg");
            while (true) {
                cnt.suspend();
            }
        };
        Coroutine failActor = (cnt) -> {
            failCalled.setTrue();
        };

        Simulator fixture = new Simulator();
        fixture.addTimer("timer", Instant.ofEpochMilli(0L));
        fixture.addActor("test", triggerTimerActor, Duration.ZERO, Instant.ofEpochMilli(0L), "sendmsg");

        fixture.removeActor("test", Instant.ofEpochMilli(1L));
        fixture.removeTimer("timer", Instant.ofEpochMilli(1L));

        fixture.addTimer("timer", Instant.ofEpochMilli(2L));
        fixture.addActor("test", failActor, Duration.ZERO, Instant.ofEpochMilli(2L));

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
        fixture.addActor("actor", actor, Duration.ZERO, Instant.EPOCH, "msg");
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
        fixture.addActor("actor", actor, Duration.ZERO, Instant.EPOCH);
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
        fixture.addActor("actor", actor, Duration.ZERO, Instant.EPOCH);

        exception.expect(IllegalArgumentException.class);
        while (fixture.hasMore()) {
            fixture.process();
        }
    }
}
