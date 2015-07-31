package com.offbynull.peernetic.core.actor.helpers;

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
        testHarness.addCoroutineActor("rcvr0", cnt -> { // ignore first 0 msgs
            Context ctx = (Context) cnt.getContext();
            while (true) {
                ctx.addOutgoingMessage(ctx.getSource(), "resp0");
                cnt.suspend();
            }
        }, Duration.ZERO, Instant.ofEpochMilli(0L));
        testHarness.addCoroutineActor("rcvr1", cnt -> { // ignore first 1 msgs
            Context ctx = (Context) cnt.getContext();
            cnt.suspend();
            while (true) {
                ctx.addOutgoingMessage(ctx.getSource(), "resp1");
                cnt.suspend();
            }
        }, Duration.ZERO, Instant.ofEpochMilli(0L));
        testHarness.addCoroutineActor("rcvr2", cnt -> { // ignore first 2 msgs
            Context ctx = (Context) cnt.getContext();
            cnt.suspend();
            cnt.suspend();
            while (true) {
                ctx.addOutgoingMessage(ctx.getSource(), "resp2");
                cnt.suspend();
            }
        }, Duration.ZERO, Instant.ofEpochMilli(0L));
        testHarness.addCoroutineActor("test", cnt -> {
            MultiRequestSubcoroutine<String> fixture = new MultiRequestSubcoroutine.Builder<String>()
                    .address(Address.of("fakeid"))
                    .request("reqmsg")
                    .addDestination("0", Address.of("rcvr0"))
                    .addDestination("1", Address.of("rcvr1"))
                    .addDestination("2", Address.of("rcvr2"))
                    .addDestination("3", Address.of("rcvr3"))
                    .addDestination("4", Address.of("rcvr4"))
                    .addDestination("5", Address.of("rcvr5"))
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

        testHarness.addCoroutineActor("test", cnt -> {
            MultiRequestSubcoroutine<String> fixture = new MultiRequestSubcoroutine.Builder<String>()
                    .address(Address.of("fakeid"))
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
