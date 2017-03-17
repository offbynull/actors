package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.MultiRequestSubcoroutine.Response;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.simulator.Simulator;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.apache.commons.lang3.mutable.MutableObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class MultiRequestSubcoroutineTest {

    // Send 6 requests, only 3 of which will respond (the other 3 addresses point to nowhere)
    @Test
    public void mustSendRequestAndCollectResponsesFromAllActiveRecipients() throws Exception {
        MutableObject<List<Response<String>>> savedResponses = new MutableObject<>();
        
        Simulator testHarness = new Simulator();
        testHarness.addTimer("timer", Instant.ofEpochMilli(0L));
        testHarness.addActor("rcvr0", (Continuation cnt) -> { // ignore first 0 msgs
            Context ctx = (Context) cnt.getContext();
            while (true) {
                ctx.out(ctx.source(), "resp0");
                cnt.suspend();
            }
        }, Duration.ZERO, Instant.ofEpochMilli(0L));
        testHarness.addActor("rcvr1", (Continuation cnt) -> { // ignore first 1 msgs
            Context ctx = (Context) cnt.getContext();
            cnt.suspend();
            while (true) {
                ctx.out(ctx.source(), "resp1");
                cnt.suspend();
            }
        }, Duration.ZERO, Instant.ofEpochMilli(0L));
        testHarness.addActor("rcvr2", (Continuation cnt) -> { // ignore first 2 msgs
            Context ctx = (Context) cnt.getContext();
            cnt.suspend();
            cnt.suspend();
            while (true) {
                ctx.out(ctx.source(), "resp2");
                cnt.suspend();
            }
        }, Duration.ZERO, Instant.ofEpochMilli(0L));
        testHarness.addActor("test", (Continuation cnt) -> {
            MultiRequestSubcoroutine<String> fixture = new MultiRequestSubcoroutine.Builder<String>()
                    .sourceAddress(Address.of("fakeid"))
                    .request("reqmsg")
                    .addDestinationAddress("0", Address.of("rcvr0"))
                    .addDestinationAddress("1", Address.of("rcvr1"))
                    .addDestinationAddress("2", Address.of("rcvr2"))
                    .addDestinationAddress("3", Address.of("rcvr3"))
                    .addDestinationAddress("4", Address.of("rcvr4"))
                    .addDestinationAddress("5", Address.of("rcvr5"))
                    .timerAddress(Address.of("timer"))
                    .addExpectedResponseType(String.class)
                    .build();
            List<Response<String>> resps = fixture.run(cnt);
            savedResponses.setValue(resps);
        }, Duration.ZERO, Instant.ofEpochMilli(0L), "start");

        while (testHarness.hasMore()) {
            testHarness.process();
        }

        List<Response<String>> resps = savedResponses.getValue();
        assertEquals(3, resps.size());
        for (int i = 0; i < 3; i++) {
            Response<String> resp = resps.get(i);
            assertEquals("resp" + i, resp.getResponse());
            assertEquals("" + i, resp.getUniqueSourceAddressSuffix());
            assertEquals(Address.fromString("fakeid:mrsr:" + i), resp.getDestinationAddress());
        }
    }

    @Test
    public void mustNotFailIfNoRecipients() throws Exception {
        MutableObject<List<Response<String>>> savedResponses = new MutableObject<>();
        
        Simulator testHarness = new Simulator();
        testHarness.addTimer("timer", Instant.ofEpochMilli(0L));

        testHarness.addActor("test", (Continuation cnt) -> {
            MultiRequestSubcoroutine<String> fixture = new MultiRequestSubcoroutine.Builder<String>()
                    .sourceAddress(Address.of("fakeid"))
                    .request("reqmsg")
                    .timerAddress(Address.of("timer"))
                    .addExpectedResponseType(String.class)
                    .build();
            List<Response<String>> resps = fixture.run(cnt);
            savedResponses.setValue(resps);
        }, Duration.ZERO, Instant.ofEpochMilli(0L), "start");

        while (testHarness.hasMore()) {
            testHarness.process();
        }

        List<Response<String>> resps = savedResponses.getValue();
        assertTrue(resps.isEmpty());
    }

}
