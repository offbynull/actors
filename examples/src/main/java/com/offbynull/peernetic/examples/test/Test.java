/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.examples.test;

import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.ActorRunner;
import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.gateways.direct.DirectGateway;
import com.offbynull.peernetic.core.gateways.log.LogGateway;
import com.offbynull.peernetic.core.gateways.log.LogMessage;
import com.offbynull.peernetic.network.actors.udpsimulator.SimpleLine;
import com.offbynull.peernetic.network.actors.udpsimulator.StartUdpSimulator;
import com.offbynull.peernetic.network.actors.udpsimulator.UdpSimulatorCoroutine;
import com.offbynull.peernetic.core.gateways.timer.TimerGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.network.gateways.udp.UdpGateway;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.lang3.Validate;

public class Test {

    public static void main(String[] args) throws Exception {
        helloWorldTest();
//        cpuTest();
        //basicTest();
        //basicTimer();
//        basicUdp();
//        basicUnreliable();
//        basicRetry();
//        testEnvironmentTimer();
//        testEnvironmentEcho();
//        testEnvironmentRecordAndReplay();
//        testRecordAndReplay();
    }

    private static void helloWorldTest() throws InterruptedException {
        // Create coroutine actor that forwards messages to the logger
        Coroutine echoerActor = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            
            // First message is the priming message. It should be the address to the logger.
            final Address loggerAddress = (Address) ctx.getIncomingMessage();
            cnt.suspend();

            // All messages after the first message are messages that we should log and echo back.
            do {
                Object msg = ctx.getIncomingMessage();
                Address srcAddress = ctx.getSource();
                ctx.addOutgoingMessage(loggerAddress, LogMessage.debug("Received a message: {}", msg));
                ctx.addOutgoingMessage(srcAddress, "Echoing back " + msg);
                cnt.suspend();
            } while (true);
        };

        // Create the actor runner, logger gateway, and direct gateway.
        ActorRunner actorRunner = new ActorRunner("actors"); // container for actors
        LogGateway logGateway = new LogGateway("log"); // gateway that logs to slf4j
        DirectGateway directGateway = new DirectGateway("direct"); // gateway that allows allows interfacing with actors/gateways from normal java code

        // Allow the actor runner to send messages to the log gateway
        actorRunner.addOutgoingShuttle(logGateway.getIncomingShuttle());
        
        // Allow the actor runner and the direct gateway to send messages to eachother
        actorRunner.addOutgoingShuttle(directGateway.getIncomingShuttle());
        directGateway.addOutgoingShuttle(actorRunner.getIncomingShuttle());

        // Add the coroutine actor and prime it with a hello world message
        actorRunner.addCoroutineActor("echoer", echoerActor, Address.of("log"));

        
        Scanner inScanner = new Scanner(System.in);
        while(inScanner.hasNextLine()) {
            // Read next line and forward to actor
            String input = inScanner.nextLine();
            directGateway.writeMessage(Address.fromString("actors:echoer"), input);
            
            // Wait for response from actor and print out
            String response = (String) directGateway.readMessages().get(0).getMessage();
            System.out.println(response);
        }
    }

    private static void cpuTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        ActorRunner runner = new ActorRunner("runner");

        for (int i = 0; i < 3000; i++) {
            Coroutine sender = (cnt) -> {
                Context ctx = (Context) cnt.getContext();
                Address dstAddr = ctx.getIncomingMessage();

                int j = 0;
                while (true) {
                    ctx.addOutgoingMessage(dstAddr, j);
                    cnt.suspend();
                    Validate.isTrue(j == (int) ctx.getIncomingMessage());
                    j++;
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

            runner.addCoroutineActor("echoer" + i, echoer);
            runner.addCoroutineActor("sender" + i, sender, Address.fromString("runner:echoer" + +i));
        }

        latch.await();
    }

    private static void basicTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Coroutine sender = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            Address dstAddr = ctx.getIncomingMessage();

            for (int i = 0; i < 10; i++) {
                ctx.addOutgoingMessage(dstAddr, i);
                cnt.suspend();
                Validate.isTrue(i == (int) ctx.getIncomingMessage());
            }

            latch.countDown();
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

        ActorRunner echoerRunner = new ActorRunner("echoer");
        ActorRunner senderRunner = new ActorRunner("sender");

        echoerRunner.addOutgoingShuttle(senderRunner.getIncomingShuttle());
        senderRunner.addOutgoingShuttle(echoerRunner.getIncomingShuttle());
        echoerRunner.addCoroutineActor("echoer", echoer);
        senderRunner.addCoroutineActor("sender", sender, Address.fromString("echoer:echoer"));

        latch.await();
    }

    private static void basicUnreliable() throws InterruptedException {
        Coroutine sender = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            Address dstAddr = ctx.getIncomingMessage();

            for (int i = 0; i < 10; i++) {
                ctx.addOutgoingMessage(Address.of("hi"), dstAddr, i);
            }

            while (true) {
                cnt.suspend();
                System.out.println(ctx.getIncomingMessage().toString());
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

        TimerGateway timerGateway = new TimerGateway("timer");

        ActorRunner echoerRunner = new ActorRunner("echoer");
        echoerRunner.addCoroutineActor("echoer", echoer);
        echoerRunner.addCoroutineActor("proxy", new UdpSimulatorCoroutine(),
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

        ActorRunner senderRunner = new ActorRunner("sender");
        senderRunner.addCoroutineActor("sender", sender, Address.fromString("sender:proxy:echoer:proxy"));
        senderRunner.addCoroutineActor("proxy", new UdpSimulatorCoroutine(),
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

        echoerRunner.addOutgoingShuttle(senderRunner.getIncomingShuttle());
        echoerRunner.addOutgoingShuttle(timerGateway.getIncomingShuttle());

        senderRunner.addOutgoingShuttle(echoerRunner.getIncomingShuttle());
        senderRunner.addOutgoingShuttle(timerGateway.getIncomingShuttle());

        timerGateway.addOutgoingShuttle(senderRunner.getIncomingShuttle());
        timerGateway.addOutgoingShuttle(echoerRunner.getIncomingShuttle());

        Thread.sleep(10000L);
    }

    private static void basicUdp() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Coroutine sender = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            Address dstAddr = ctx.getIncomingMessage();

            for (int i = 0; i < 10; i++) {
                ctx.addOutgoingMessage(dstAddr, i);
                cnt.suspend();
                Validate.isTrue(i == (int) ctx.getIncomingMessage());
            }

            latch.countDown();
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

        ActorRunner echoerRunner = new ActorRunner("echoer");
        Shuttle echoerInputShuttle = echoerRunner.getIncomingShuttle();
        UdpGateway echoerUdpGateway = new UdpGateway(
                new InetSocketAddress(1000),
                "internaludp",
                echoerInputShuttle,
                Address.fromString("echoer:echoer"),
                new SimpleSerializer());
        Shuttle echoerUdpOutputShuttle = echoerUdpGateway.getIncomingShuttle();
        echoerRunner.addOutgoingShuttle(echoerUdpOutputShuttle);

        ActorRunner senderRunner = new ActorRunner("sender");
        Shuttle senderInputShuttle = senderRunner.getIncomingShuttle();
        UdpGateway senderUdpGateway = new UdpGateway(
                new InetSocketAddress(2000),
                "internaludp",
                senderInputShuttle,
                Address.fromString("sender:sender"),
                new SimpleSerializer());
        Shuttle senderUdpOutputShuttle = senderUdpGateway.getIncomingShuttle();
        senderRunner.addOutgoingShuttle(senderUdpOutputShuttle);

        echoerRunner.addCoroutineActor("echoer", echoer);
        senderRunner.addCoroutineActor("sender", sender, Address.fromString("internaludp:7f000001.1000"));

        latch.await();
    }

//    private static void basicTimer() throws InterruptedException {
//        CountDownLatch latch = new CountDownLatch(1);
//
//        Coroutine tester = (cnt) -> {
//            Context ctx = (Context) cnt.getContext();
//
//            String timerPrefix = ctx.getIncomingMessage();
//            ctx.addOutgoingMessage("fromid", timerPrefix + ":2000:extra", 0);
//            System.out.println("ADDED TRIGGER FOR FOR 2 SECOND");
//            cnt.suspend();
//            System.out.println("TRIGGERED FROM " + ctx.getSource()+ " TO " + ctx.getDestination()+ " WITH " + ctx.getIncomingMessage());
//
//            latch.countDown();
//        };
//
//        TimerGateway timerGateway = new TimerGateway("timer");
//        Shuttle timerInputShuttle = timerGateway.getIncomingShuttle();
//
//        ActorRunner testerRunner = ActorRunner.create("local");
//        Shuttle testerInputShuttle = testerRunner.getIncomingShuttle();
//
//        testerRunner.addOutgoingShuttle(timerInputShuttle);
//        timerGateway.addOutgoingShuttle(testerInputShuttle);
//
//        testerRunner.addCoroutineActor("tester", tester, "timer");
//
//        latch.await();
//    }
//    private static void basicRetry() throws InterruptedException {
//        CountDownLatch latch = new CountDownLatch(1);
//
//        Coroutine sender = (cnt) -> {
//            Context ctx = (Context) cnt.getContext();
//            String dstAddr = ctx.getIncomingMessage();
//
//            for (int i = 0; i < 10; i++) {
//                ctx.addOutgoingMessage("hi", dstAddr, i);
//                System.out.println("sending msg " + i + " to " + dstAddr);
//                cnt.suspend();
//                Validate.isTrue(i == (int) ctx.getIncomingMessage());
//                System.out.println("recvd response from " + ctx.getSource() + " to " + ctx.getDestination() + " with value " + i);
//            }
//
//            latch.countDown();
//        };
//
//        Coroutine echoer = (cnt) -> {
//            Context ctx = (Context) cnt.getContext();
//
//            while (true) {
//                String src = ctx.getSource();
//                Object msg = ctx.getIncomingMessage();
//                System.out.println("echoing msg from " + src + " with value " + msg);
//                ctx.addOutgoingMessage(src, msg);
//                cnt.suspend();
//            }
//        };
//
//        ActorRunner echoerRunner = ActorRunner.create("echoer");
//        ActorRunner senderRunner = ActorRunner.create("sender");
//        TimerGateway timerGateway = new TimerGateway("timer");
//
//        // This is slow because the retry durations are very far apart in SimpleSendGuidelineGenerator/SimpleReceiveGuidelineGenerator, but
//        // it will eventually finish!
//        echoerRunner.addCoroutineActor("unreliable", new UdpSimulatorCoroutine(),
//                new StartUdpSimulator("timer", "echoer:retry", new SimpleLine(0L, Duration.ofMillis(100L), Duration.ofMillis(500L), 0.25, 0.25, 10)));
//        echoerRunner.addCoroutineActor("retry", new RetryProxyCoroutine(),
//                new StartRetryProxy("timer", "echoer:echoer", x -> x.toString(), new SimpleSendGuidelineGenerator(), new SimpleReceiveGuidelineGenerator()));
//        echoerRunner.addCoroutineActor("echoer", echoer);
//        
//        senderRunner.addCoroutineActor("unreliable", new UdpSimulatorCoroutine(),
//                new StartUdpSimulator("timer", "sender:retry", new SimpleLine(0L, Duration.ofMillis(100L), Duration.ofMillis(500L), 0.25, 0.25, 10)));
//        senderRunner.addCoroutineActor("retry", new RetryProxyCoroutine(),
//                new StartRetryProxy("timer", "sender:sender", x -> x.toString(), new SimpleSendGuidelineGenerator(), new SimpleReceiveGuidelineGenerator()));
//        senderRunner.addCoroutineActor("sender", sender, "sender:retry:echoer:unreliable"); // needs to be added last to avoid race condition
//
//        echoerRunner.addOutgoingShuttle(senderRunner.getIncomingShuttle());
//        echoerRunner.addOutgoingShuttle(timerGateway.getIncomingShuttle());
//        senderRunner.addOutgoingShuttle(echoerRunner.getIncomingShuttle());
//        senderRunner.addOutgoingShuttle(timerGateway.getIncomingShuttle());
//        timerGateway.addOutgoingShuttle(senderRunner.getIncomingShuttle());
//        timerGateway.addOutgoingShuttle(echoerRunner.getIncomingShuttle());
//        
//        latch.await();
//    }
//    private static void testEnvironmentTimer() {
//        Coroutine tester = (cnt) -> {
//            Context ctx = (Context) cnt.getContext();
//
//            System.out.println("Sending out at " + ctx.getTime());
//            String timerPrefix = ctx.getIncomingMessage();
//            ctx.addOutgoingMessage(timerPrefix + ":2000", 0);
//            cnt.suspend();
//            System.out.println("Got response at " + ctx.getTime());
//        };
//
//        Simulator testHarness = new Simulator();
//        testHarness.addTimer("timer", Instant.ofEpochMilli(0L));
//        testHarness.addCoroutineActor("local", tester, Duration.ZERO, Instant.ofEpochMilli(0L), "timer");
//        
//        while (testHarness.hasMore()) {
//            testHarness.process();
//        }
//    }
//    private static void testEnvironmentEcho() {
//        Coroutine sender = (cnt) -> {
//            Context ctx = (Context) cnt.getContext();
//            String dstAddr = ctx.getIncomingMessage();
//
//            for (int i = 0; i < 10; i++) {
//                ctx.addOutgoingMessage(dstAddr, i);
//                cnt.suspend();
//                Validate.isTrue(i == (int) ctx.getIncomingMessage());
//                System.out.println("Got " + i);
//            }
//        };
//
//        Coroutine echoer = (cnt) -> {
//            Context ctx = (Context) cnt.getContext();
//
//            while (true) {
//                String src = ctx.getSource();
//                Object msg = ctx.getIncomingMessage();
//                ctx.addOutgoingMessage(src, msg);
//                cnt.suspend();
//            }
//        };
//
//        Simulator testHarness = new Simulator();
//        testHarness.addTimer("timer", Instant.ofEpochMilli(0L));
//        testHarness.addCoroutineActor("local:sender", sender, Duration.ZERO, Instant.ofEpochMilli(0L), "local:echoer");
//        testHarness.addCoroutineActor("local:echoer", echoer, Duration.ZERO, Instant.ofEpochMilli(0L));
//        
//        while (testHarness.hasMore()) {
//            testHarness.process();
//        }
//    }
//    private static void testEnvironmentRecordAndReplay() throws Exception {
//        File recordFile = File.createTempFile("recordtest", ".data");
//        
//        System.out.println("RECORDING ECHO");
//        {
//            Coroutine sender = (cnt) -> {
//                Context ctx = (Context) cnt.getContext();
//                String dstAddr = ctx.getIncomingMessage();
//
//                for (int i = 0; i < 10; i++) {
//                    ctx.addOutgoingMessage(dstAddr, i);
//                    cnt.suspend();
//                    Validate.isTrue(i == (int) ctx.getIncomingMessage());
//                    System.out.println("Got " + i);
//                }
//            };
//
//            Coroutine echoer = (cnt) -> {
//                Context ctx = (Context) cnt.getContext();
//
//                while (true) {
//                    String src = ctx.getSource();
//                    Object msg = ctx.getIncomingMessage();
//                    ctx.addOutgoingMessage(src, msg);
//                    cnt.suspend();
//                }
//            };
//
//            try (MessageSink sink = new RecordMessageSink("local:echoer", recordFile, new SimpleSerializer())) {
//                Simulator testHarness = new Simulator();
//                testHarness.addTimer("timer", Instant.ofEpochMilli(0L));
//                testHarness.addMessageSink(sink, Instant.ofEpochMilli(0L));
//                testHarness.addCoroutineActor("local:sender", sender, Duration.ZERO, Instant.ofEpochMilli(0L), "local:echoer");
//                testHarness.addCoroutineActor("local:echoer", echoer, Duration.ZERO, Instant.ofEpochMilli(0L));
//
//                while (testHarness.hasMore()) {
//                    testHarness.process();
//                }
//            }
//        }
//        
//        
//        System.out.println("REPLAYING RECORDING OF ECHO");
//        {
//            Coroutine sender = (cnt) -> {
//                Context ctx = (Context) cnt.getContext();
//                String dstAddr = ctx.getIncomingMessage();
//
//                for (int i = 0; i < 10; i++) {
//                    ctx.addOutgoingMessage(dstAddr, i);
//                    cnt.suspend();
//                    Validate.isTrue(i == (int) ctx.getIncomingMessage());
//                    System.out.println("Got " + i);
//                }
//            };
//
//            try (MessageSource source = new ReplayMessageSource("local:sender", recordFile, new SimpleSerializer())) {
//                Simulator testHarness = new Simulator();
//                testHarness.addTimer("timer", Instant.ofEpochMilli(0L));
//                testHarness.addCoroutineActor("local:sender", sender, Duration.ZERO, Instant.ofEpochMilli(0L), "local:echoer");
//                testHarness.addMessageSource(source, Instant.ofEpochMilli(0L));
//
//                while (testHarness.hasMore()) {
//                    testHarness.process();
//                }
//            }
//        }
//    }
//    private static void testRecordAndReplay() throws InterruptedException, IOException {
//        File eventsFile = File.createTempFile(Test.class.getSimpleName(), "data");
//        
//        // RUN SENDER+ECHOER WITH EVENTS COMING IN TO ECHOER BEING RECORDED
//        {
//            System.out.println("RECORD RUN");
//            
//            CountDownLatch latch = new CountDownLatch(1);
//
//            Coroutine sender = (cnt) -> {
//                Context ctx = (Context) cnt.getContext();
//                String dstAddr = ctx.getIncomingMessage();
//
//                for (int i = 0; i < 10; i++) {
//                    ctx.addOutgoingMessage(dstAddr, i);
//                    cnt.suspend();
//                    Validate.isTrue(i == (int) ctx.getIncomingMessage());
//                }
//
//                latch.countDown();
//            };
//
//            Coroutine echoer = (cnt) -> {
//                Context ctx = (Context) cnt.getContext();
//
//                while (true) {
//                    String src = ctx.getSource();
//                    Object msg = ctx.getIncomingMessage();
//                    ctx.addOutgoingMessage(src, msg);
//                    System.out.println(msg);
//                    cnt.suspend();
//                }
//            };
//
//            // Create actor runners
//            ActorRunner echoerRunner = ActorRunner.create("echoer");
//            ActorRunner senderRunner = ActorRunner.create("sender");
//
//            // Create recorder that records events coming to echoer and then passes it along to echoer
//            RecorderGateway echoRecorderGateway = RecorderGateway.record(
//                    "recorder",
//                    echoerRunner.getIncomingShuttle(),
//                    "echoer:echoer",
//                    eventsFile,
//                    new SimpleSerializer());
//            Shuttle echoRecorderShuttle = echoRecorderGateway.getIncomingShuttle();
//
//
//            // Wire sender to send to echoerRecorder instead of echoer
//            senderRunner.addOutgoingShuttle(echoRecorderShuttle);
//
//            // Wire echoer to send back directly to recorder
//            echoerRunner.addOutgoingShuttle(senderRunner.getIncomingShuttle());
//
//            // Add coroutines
//            echoerRunner.addCoroutineActor("echoer", echoer);
//            senderRunner.addCoroutineActor("sender", sender, "recorder");
//
//            latch.await();
//            echoRecorderGateway.close();
//            echoRecorderGateway.await();
//        }
//        
//        
//        // RUN ECHOER WITH EVENTS COMING IN FROM SAVED EVENTS BEING REPLAYED
//        {
//            System.out.println("REPLAY RUN");
//            
//            Coroutine echoer = (cnt) -> {
//                Context ctx = (Context) cnt.getContext();
//
//                while (true) {
//                    String src = ctx.getSource();
//                    Object msg = ctx.getIncomingMessage();
//                    ctx.addOutgoingMessage(src, msg);
//                    System.out.println(msg);
//                    cnt.suspend();
//                }
//            };
//            
//            ActorRunner echoerRunner = ActorRunner.create("echoer");
//            
//            // Wire echoer to send back to null
//            echoerRunner.addOutgoingShuttle(new NullShuttle("sender"));
//            
//            // Add coroutines
//            echoerRunner.addCoroutineActor("echoer", echoer);
//            
//            // Create replayer that mocks out sender and replays previous events to echoer
//            ReplayerGateway replayerGateway = ReplayerGateway.replay(
//                    echoerRunner.getIncomingShuttle(),
//                    "echoer:echoer",
//                    eventsFile,
//                    new SimpleSerializer());
//            replayerGateway.await();
//        }
//    }
}
