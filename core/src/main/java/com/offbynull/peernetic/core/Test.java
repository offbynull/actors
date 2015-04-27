package com.offbynull.peernetic.core;

import com.offbynull.peernetic.core.shuttles.test.NullShuttle;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.core.actors.unreliable.SimpleLine;
import com.offbynull.peernetic.core.actors.unreliable.StartUnreliableProxy;
import com.offbynull.peernetic.core.actors.unreliable.UnreliableProxyCoroutine;
import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.gateways.recorder.RecorderGateway;
import com.offbynull.peernetic.core.gateways.recorder.ReplayerGateway;
import com.offbynull.peernetic.core.gateways.timer.TimerGateway;
import com.offbynull.peernetic.core.test.TestHarness;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.lang3.Validate;

public class Test {

    public static void main(String[] args) throws Exception {
        basicTest();
        basicTimer();
//        basicUdp();
        basicUnreliable();
//        basicRetry();
        testEnvironmentTimer();
        testEnvironmentEcho();
        testRecordAndReplay();
    }

    private static void basicTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Coroutine sender = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            String dstAddr = ctx.getIncomingMessage();

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
                String src = ctx.getSource();
                Object msg = ctx.getIncomingMessage();
                ctx.addOutgoingMessage(src, msg);
                cnt.suspend();
            }
        };

        ActorThread echoerThread = ActorThread.create("echoer");
        ActorThread senderThread = ActorThread.create("sender");

        echoerThread.addOutgoingShuttle(senderThread.getIncomingShuttle());
        senderThread.addOutgoingShuttle(echoerThread.getIncomingShuttle());
        echoerThread.addCoroutineActor("echoer", echoer);
        senderThread.addCoroutineActor("sender", sender, "echoer:echoer");

        latch.await();
    }

    private static void basicUnreliable() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Coroutine sender = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            String dstAddr = ctx.getIncomingMessage();

            for (int i = 0; i < 10; i++) {
                ctx.addOutgoingMessage("hi", dstAddr, i);
                cnt.suspend();
                Validate.isTrue(i == (int) ctx.getIncomingMessage());
            }

            latch.countDown();
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

        ActorThread echoerThread = ActorThread.create("echoer");
        ActorThread senderThread = ActorThread.create("sender");
        TimerGateway timerGateway = new TimerGateway("timer");

        echoerThread.addCoroutineActor("echoer", echoer);
        echoerThread.addCoroutineActor("proxy", new UnreliableProxyCoroutine(),
                new StartUnreliableProxy("timer", "echoer:echoer", new SimpleLine(12345L)));
        senderThread.addCoroutineActor("sender", sender, "sender:proxy:echoer:echoer");
        senderThread.addCoroutineActor("proxy", new UnreliableProxyCoroutine(),
                new StartUnreliableProxy("timer", "sender:sender", new SimpleLine(12345L)));

        echoerThread.addOutgoingShuttle(senderThread.getIncomingShuttle());
        echoerThread.addOutgoingShuttle(timerGateway.getIncomingShuttle());
        senderThread.addOutgoingShuttle(echoerThread.getIncomingShuttle());
        senderThread.addOutgoingShuttle(timerGateway.getIncomingShuttle());
        timerGateway.addOutgoingShuttle(senderThread.getIncomingShuttle());
        timerGateway.addOutgoingShuttle(echoerThread.getIncomingShuttle());
        
        latch.await();
    }

//    private static void basicUdp() throws InterruptedException {
//        CountDownLatch latch = new CountDownLatch(1);
//
//        Coroutine sender = (cnt) -> {
//            Context ctx = (Context) cnt.getContext();
//            String dstAddr = ctx.getIncomingMessage();
//
//            for (int i = 0; i < 10; i++) {
//                ctx.addOutgoingMessage(dstAddr, i);
//                cnt.suspend();
//                Validate.isTrue(i == (int) ctx.getIncomingMessage());
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
//                ctx.addOutgoingMessage(src, msg);
//                cnt.suspend();
//            }
//        };
//
//        ActorThread echoerThread = ActorThread.create("echoer");
//        Shuttle echoerInputShuttle = echoerThread.getIncomingShuttle();
//        Gateway echoerUdpGateway = new UdpGateway(new InetSocketAddress(1000), "internaludp", echoerInputShuttle, "echoer:echoer", new SimpleSerializer());
//        Shuttle echoerOutputShuttle = echoerUdpGateway.getIncomingShuttle();
//
//        ActorThread senderThread = ActorThread.create("sender");
//        Shuttle senderInputShuttle = senderThread.getIncomingShuttle();
//        Gateway senderUdpGateway = new UdpGateway(new InetSocketAddress(2000), "internaludp", senderInputShuttle, "sender:sender", new SimpleSerializer());
//        Shuttle senderOutputShuttle = senderUdpGateway.getIncomingShuttle();
//
//        echoerThread.addOutgoingShuttle(echoerOutputShuttle);
//        senderThread.addOutgoingShuttle(senderOutputShuttle);
//
//        echoerThread.addCoroutineActor("echoer", echoer);
//        senderThread.addCoroutineActor("sender", sender, "internaludp:7f000001.1000");
//
//        latch.await();
//    }

    private static void basicTimer() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Coroutine tester = (cnt) -> {
            Context ctx = (Context) cnt.getContext();

            String timerPrefix = ctx.getIncomingMessage();
            ctx.addOutgoingMessage("fromid", timerPrefix + ":2000:extra", 0);
            System.out.println("ADDED TRIGGER FOR FOR 2 SECOND");
            cnt.suspend();
            System.out.println("TRIGGERED FROM " + ctx.getSource()+ " TO " + ctx.getDestination()+ " WITH " + ctx.getIncomingMessage());

            latch.countDown();
        };

        TimerGateway timerGateway = new TimerGateway("timer");
        Shuttle timerInputShuttle = timerGateway.getIncomingShuttle();

        ActorThread testerThread = ActorThread.create("local");
        Shuttle testerInputShuttle = testerThread.getIncomingShuttle();

        testerThread.addOutgoingShuttle(timerInputShuttle);
        timerGateway.addOutgoingShuttle(testerInputShuttle);

        testerThread.addCoroutineActor("tester", tester, "timer");

        latch.await();
    }

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
//        ActorThread echoerThread = ActorThread.create("echoer");
//        ActorThread senderThread = ActorThread.create("sender");
//        TimerGateway timerGateway = new TimerGateway("timer");
//
//        // This is slow because the retry durations are very far apart in SimpleSendGuidelineGenerator/SimpleReceiveGuidelineGenerator, but
//        // it will eventually finish!
//        echoerThread.addCoroutineActor("unreliable", new UnreliableProxyCoroutine(),
//                new StartUnreliableProxy("timer", "echoer:retry", new SimpleLine(0L, Duration.ofMillis(100L), Duration.ofMillis(500L), 0.25, 0.25, 10)));
//        echoerThread.addCoroutineActor("retry", new RetryProxyCoroutine(),
//                new StartRetryProxy("timer", "echoer:echoer", x -> x.toString(), new SimpleSendGuidelineGenerator(), new SimpleReceiveGuidelineGenerator()));
//        echoerThread.addCoroutineActor("echoer", echoer);
//        
//        senderThread.addCoroutineActor("unreliable", new UnreliableProxyCoroutine(),
//                new StartUnreliableProxy("timer", "sender:retry", new SimpleLine(0L, Duration.ofMillis(100L), Duration.ofMillis(500L), 0.25, 0.25, 10)));
//        senderThread.addCoroutineActor("retry", new RetryProxyCoroutine(),
//                new StartRetryProxy("timer", "sender:sender", x -> x.toString(), new SimpleSendGuidelineGenerator(), new SimpleReceiveGuidelineGenerator()));
//        senderThread.addCoroutineActor("sender", sender, "sender:retry:echoer:unreliable"); // needs to be added last to avoid race condition
//
//        echoerThread.addOutgoingShuttle(senderThread.getIncomingShuttle());
//        echoerThread.addOutgoingShuttle(timerGateway.getIncomingShuttle());
//        senderThread.addOutgoingShuttle(echoerThread.getIncomingShuttle());
//        senderThread.addOutgoingShuttle(timerGateway.getIncomingShuttle());
//        timerGateway.addOutgoingShuttle(senderThread.getIncomingShuttle());
//        timerGateway.addOutgoingShuttle(echoerThread.getIncomingShuttle());
//        
//        latch.await();
//    }

    private static void testEnvironmentTimer() {
        Coroutine tester = (cnt) -> {
            Context ctx = (Context) cnt.getContext();

            System.out.println("Sending out at " + ctx.getTime());
            String timerPrefix = ctx.getIncomingMessage();
            ctx.addOutgoingMessage(timerPrefix + ":2000", 0);
            cnt.suspend();
            System.out.println("Got response at " + ctx.getTime());
        };

        TestHarness testHarness = new TestHarness("timer");
        testHarness.addCoroutineActor("local", tester, Duration.ZERO, Instant.ofEpochMilli(0L), "timer");
        
        while (testHarness.hasMore()) {
            testHarness.process();
        }
    }

    private static void testEnvironmentEcho() {
        Coroutine sender = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            String dstAddr = ctx.getIncomingMessage();

            for (int i = 0; i < 10; i++) {
                ctx.addOutgoingMessage(dstAddr, i);
                cnt.suspend();
                Validate.isTrue(i == (int) ctx.getIncomingMessage());
                System.out.println("Got " + i);
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

        TestHarness testHarness = new TestHarness("timer");
        testHarness.addCoroutineActor("local:sender", sender, Duration.ZERO, Instant.ofEpochMilli(0L), "local:echoer");
        testHarness.addCoroutineActor("local:echoer", echoer, Duration.ZERO, Instant.ofEpochMilli(0L));
        
        while (testHarness.hasMore()) {
            testHarness.process();
        }
    }
    
    private static void testRecordAndReplay() throws InterruptedException, IOException {
        File eventsFile = File.createTempFile(Test.class.getSimpleName(), "data");
        
        // RUN SENDER+ECHOER WITH EVENTS COMING IN TO ECHOER BEING RECORDED
        {
            System.out.println("RECORD RUN");
            
            CountDownLatch latch = new CountDownLatch(1);

            Coroutine sender = (cnt) -> {
                Context ctx = (Context) cnt.getContext();
                String dstAddr = ctx.getIncomingMessage();

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
                    String src = ctx.getSource();
                    Object msg = ctx.getIncomingMessage();
                    ctx.addOutgoingMessage(src, msg);
                    System.out.println(msg);
                    cnt.suspend();
                }
            };

            // Create actor threads
            ActorThread echoerThread = ActorThread.create("echoer");
            ActorThread senderThread = ActorThread.create("sender");

            // Create recorder that records events coming to echoer and then passes it along to echoer
            RecorderGateway echoRecorderGateway = RecorderGateway.record(
                    "recorder",
                    echoerThread.getIncomingShuttle(),
                    "echoer:echoer",
                    eventsFile,
                    new SimpleSerializer());
            Shuttle echoRecorderShuttle = echoRecorderGateway.getIncomingShuttle();


            // Wire sender to send to echoerRecorder instead of echoer
            senderThread.addOutgoingShuttle(echoRecorderShuttle);

            // Wire echoer to send back directly to recorder
            echoerThread.addOutgoingShuttle(senderThread.getIncomingShuttle());

            // Add coroutines
            echoerThread.addCoroutineActor("echoer", echoer);
            senderThread.addCoroutineActor("sender", sender, "recorder");

            latch.await();
            echoRecorderGateway.close();
            echoRecorderGateway.await();
        }
        
        
        // RUN ECHOER WITH EVENTS COMING IN FROM SAVED EVENTS BEING REPLAYED
        {
            System.out.println("REPLAY RUN");
            
            Coroutine echoer = (cnt) -> {
                Context ctx = (Context) cnt.getContext();

                while (true) {
                    String src = ctx.getSource();
                    Object msg = ctx.getIncomingMessage();
                    ctx.addOutgoingMessage(src, msg);
                    System.out.println(msg);
                    cnt.suspend();
                }
            };
            
            ActorThread echoerThread = ActorThread.create("echoer");
            
            // Wire echoer to send back to null
            echoerThread.addOutgoingShuttle(new NullShuttle("sender"));
            
            // Add coroutines
            echoerThread.addCoroutineActor("echoer", echoer);
            
            // Create replayer that mocks out sender and replays previous events to echoer
            ReplayerGateway replayerGateway = ReplayerGateway.replay(
                    echoerThread.getIncomingShuttle(),
                    "echoer:echoer",
                    eventsFile,
                    new SimpleSerializer());
            replayerGateway.await();
        }
    }
}
