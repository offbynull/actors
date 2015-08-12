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
import org.apache.commons.lang3.mutable.MutableObject;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class RequestSubcoroutineTest {

    @Test
    public void mustSendRequestOnlyOnce() throws Exception {
        List<String> srcAddresses = new ArrayList<>();
        List<String> dstAddresses = new ArrayList<>();
        List<Object> recvdItems = new ArrayList<>();
        
        Simulator testHarness = new Simulator();
        testHarness.addTimer("timer", Instant.ofEpochMilli(0L));
        testHarness.addActor("rcvr", (Continuation cnt) -> {
            Context ctx = (Context) cnt.getContext();
            srcAddresses.add(ctx.getSource().toString());
            dstAddresses.add(ctx.getDestination().toString());
            recvdItems.add(ctx.getIncomingMessage());
        }, Duration.ZERO, Instant.ofEpochMilli(0L));
        testHarness.addActor("test", (Continuation cnt) -> {
            RequestSubcoroutine<String> fixture = new RequestSubcoroutine.Builder<String>()
                    .sourceAddress(Address.of("fakeid"))
                    .request("reqmsg")
                    .destinationAddress(Address.of("rcvr"))
                    .timerAddress(Address.of("timer"))
                    .throwExceptionIfNoResponse(false)
                    .maxAttempts(1)
                    .attemptInterval(Duration.ZERO)
                    .build();
            fixture.run(cnt);
        }, Duration.ZERO, Instant.ofEpochMilli(0L), "start");

        while (testHarness.hasMore()) {
            testHarness.process();
        }

        assertEquals(Arrays.asList("test:fakeid"), srcAddresses);
        assertEquals(Arrays.asList("rcvr"), dstAddresses);
        assertEquals(Arrays.asList("reqmsg"), recvdItems);
    }

    @Test
    public void mustSendRequestMultipleTimes() throws Exception {
        List<String> srcAddresses = new ArrayList<>();
        List<String> dstAddresses = new ArrayList<>();
        List<Object> recvdItems = new ArrayList<>();
        
        Simulator testHarness = new Simulator();
        testHarness.addTimer("timer", Instant.ofEpochMilli(0L));
        testHarness.addActor("rcvr", (Continuation cnt) -> {
            Context ctx = (Context) cnt.getContext();
            while (true) {
                srcAddresses.add(ctx.getSource().toString());
                dstAddresses.add(ctx.getDestination().toString());
                recvdItems.add(ctx.getIncomingMessage());
                cnt.suspend();
            }
        }, Duration.ZERO, Instant.ofEpochMilli(0L));
        testHarness.addActor("test", (Continuation cnt) -> {
            RequestSubcoroutine<String> fixture = new RequestSubcoroutine.Builder<String>()
                    .sourceAddress(Address.of("fakeid"))
                    .request("reqmsg")
                    .destinationAddress(Address.of("rcvr"))
                    .timerAddress(Address.of("timer"))
                    .throwExceptionIfNoResponse(false)
                    .build();
            fixture.run(cnt);
        }, Duration.ZERO, Instant.ofEpochMilli(0L), "start");

        while (testHarness.hasMore()) {
            testHarness.process();
        }

        assertEquals(Collections.nCopies(5, "test:fakeid"), srcAddresses);
        assertEquals(Collections.nCopies(5, "rcvr"), dstAddresses);
        assertEquals(Collections.nCopies(5, "reqmsg"), recvdItems);
    }

    @Test
    public void mustStopResendingRequestWhenResponseArrives() throws Exception {
        List<String> srcAddresses = new ArrayList<>();
        List<String> dstAddresses = new ArrayList<>();
        List<Object> recvdItems = new ArrayList<>();
        MutableObject<String> savedResponse = new MutableObject<>();
        
        Simulator testHarness = new Simulator();
        testHarness.addTimer("timer", Instant.ofEpochMilli(0L));
        testHarness.addActor("rcvr", (Continuation cnt) -> {
            Context ctx = (Context) cnt.getContext();
            while (true) {
                srcAddresses.add(ctx.getSource().toString());
                dstAddresses.add(ctx.getDestination().toString());
                recvdItems.add(ctx.getIncomingMessage());
                
                ctx.addOutgoingMessage(ctx.getSource(), "resp");
                cnt.suspend();
            }
        }, Duration.ZERO, Instant.ofEpochMilli(0L));
        testHarness.addActor("test", (Continuation cnt) -> {
            RequestSubcoroutine<String> fixture = new RequestSubcoroutine.Builder<String>()
                    .sourceAddress(Address.of("fakeid"))
                    .request("reqmsg")
                    .destinationAddress(Address.of("rcvr"))
                    .timerAddress(Address.of("timer"))
                    .throwExceptionIfNoResponse(false)
                    .addExpectedResponseType(String.class)
                    .build();
            String resp = fixture.run(cnt);
            savedResponse.setValue(resp);
        }, Duration.ZERO, Instant.ofEpochMilli(0L), "start");

        while (testHarness.hasMore()) {
            testHarness.process();
        }

        assertEquals(Arrays.asList("test:fakeid"), srcAddresses);
        assertEquals(Arrays.asList("rcvr"), dstAddresses);
        assertEquals(Arrays.asList("reqmsg"), recvdItems);
        assertEquals("resp", savedResponse.getValue());
    }

}
