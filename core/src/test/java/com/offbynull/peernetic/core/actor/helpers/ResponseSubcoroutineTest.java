package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.simulator.Simulator;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class ResponseSubcoroutineTest {

    @Test
    public void mustIgnoreSubsequentMessagesFromSameSource() {

        Simulator testHarness = new Simulator();
        testHarness.addTimer("timer", Instant.ofEpochMilli(0L));
        CaptureSubcoroutine captureSubcoroutine = new CaptureSubcoroutine();
        testHarness.addCoroutineActor(
                "rcvr",
                cnt -> {
                    new ResponseSubcoroutine<>(
                            Address.of("timer"),     // address of timer used for removing items from cache
                            Duration.ofSeconds(10L), // 10 second cache duration
                            captureSubcoroutine      // subcoroutine to forward to
                    ).run(cnt);
                },
                Duration.ZERO,
                Instant.ofEpochMilli(0L)
        );
        testHarness.addCoroutineActor("test", cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.addOutgoingMessage(Address.of("fakeid"), Address.of("rcvr"), "0");
            ctx.addOutgoingMessage(Address.of("fakeid"), Address.of("rcvr"), "1");
            ctx.addOutgoingMessage(Address.of("fakeid"), Address.of("rcvr"), "2");
            ctx.addOutgoingMessage(Address.of("fakeid"), Address.of("rcvr"), "3");
            ctx.addOutgoingMessage(Address.of("fakeid"), Address.of("rcvr"), "4");
        }, Duration.ZERO, Instant.ofEpochMilli(0L), "start");

        while (testHarness.hasMore()) {
            testHarness.process();
        }

        assertEquals(Collections.nCopies(1, "test:fakeid"), captureSubcoroutine.srcAddresses);
        assertEquals(Collections.nCopies(1, "rcvr"), captureSubcoroutine.dstAddresses);
        assertEquals(Collections.nCopies(1, "0"), captureSubcoroutine.recvdItems);
    }

    @Test
    public void mustNotIgnoreSubsequentMessagesFromSameSourceIfExceedsPastDuration() {

        Simulator testHarness = new Simulator();
        testHarness.addTimer("timer", Instant.ofEpochMilli(0L));
        CaptureSubcoroutine captureSubcoroutine = new CaptureSubcoroutine();
        testHarness.addCoroutineActor(
                "rcvr",
                cnt -> {
                    new ResponseSubcoroutine<>(
                            Address.of("timer"),     // address of timer used for removing items from cache
                            Duration.ofSeconds(10L), // 10 second cache duration
                            captureSubcoroutine      // subcoroutine to forward to
                    ).run(cnt);
                },
                Duration.ZERO,
                Instant.ofEpochMilli(0L)
        );
        testHarness.addCoroutineActor("test", cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.addOutgoingMessage(Address.of("fakeid"), Address.of("rcvr"), "0");
            ctx.addOutgoingMessage(Address.of("timer", "20000"), new Object()); // wake up in 20seconds
            
            cnt.suspend();
            ctx.addOutgoingMessage(Address.of("fakeid"), Address.of("rcvr"), "1");
        }, Duration.ZERO, Instant.ofEpochMilli(0L), "start");

        while (testHarness.hasMore()) {
            testHarness.process();
        }

        assertEquals(Collections.nCopies(2, "test:fakeid"), captureSubcoroutine.srcAddresses);
        assertEquals(Collections.nCopies(2, "rcvr"), captureSubcoroutine.dstAddresses);
        assertEquals(Arrays.asList("0", "1"), captureSubcoroutine.recvdItems);
    }
    
    @Test
    public void mustNotIgnoreSubsequentMessagesIfSourceIsDifferent() {

        Simulator testHarness = new Simulator();
        testHarness.addTimer("timer", Instant.ofEpochMilli(0L));
        CaptureSubcoroutine captureSubcoroutine = new CaptureSubcoroutine();
        testHarness.addCoroutineActor(
                "rcvr",
                cnt -> {
                    new ResponseSubcoroutine<>(
                            Address.of("timer"),     // address of timer used for removing items from cache
                            Duration.ofSeconds(10L), // 10 second cache duration
                            captureSubcoroutine      // subcoroutine to forward to
                    ).run(cnt);
                },
                Duration.ZERO,
                Instant.ofEpochMilli(0L)
        );
        testHarness.addCoroutineActor("test", cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.addOutgoingMessage(Address.of("fakeid0"), Address.of("rcvr"), "0");
            ctx.addOutgoingMessage(Address.of("fakeid1"), Address.of("rcvr"), "1");
        }, Duration.ZERO, Instant.ofEpochMilli(0L), "start");

        while (testHarness.hasMore()) {
            testHarness.process();
        }

        assertEquals(Arrays.asList("test:fakeid0", "test:fakeid1"), captureSubcoroutine.srcAddresses);
        assertEquals(Collections.nCopies(2, "rcvr"), captureSubcoroutine.dstAddresses);
        assertEquals(Arrays.asList("0", "1"), captureSubcoroutine.recvdItems);
    }

    private static final class CaptureSubcoroutine implements Subcoroutine<Void> {

        private List<String> srcAddresses = new ArrayList<>();
        private List<String> dstAddresses = new ArrayList<>();
        private List<Object> recvdItems = new ArrayList<>();
        
        @Override
        public Address getId() {
            return Address.of();
        }

        @Override
        public Void run(Continuation cnt) throws Exception {
            Context ctx = (Context) cnt.getContext();
            while (true) {
                srcAddresses.add(ctx.getSource().toString());
                dstAddresses.add(ctx.getDestination().toString());
                recvdItems.add(ctx.getIncomingMessage());
                cnt.suspend();
            }
        }
    }
}
