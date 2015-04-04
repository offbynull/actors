package com.offbynull.peernetic.core;

import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.core.actors.retry.IdExtractor;
import com.offbynull.peernetic.core.actors.retry.ReceiveGuideline;
import com.offbynull.peernetic.core.actors.retry.ReceiveGuidelineGenerator;
import com.offbynull.peernetic.core.actors.retry.RetryReceiveProxyCoroutine;
import com.offbynull.peernetic.core.actors.retry.RetrySendProxyCoroutine;
import com.offbynull.peernetic.core.actors.retry.SendGuideline;
import com.offbynull.peernetic.core.actors.retry.SendGuidelineGenerator;
import com.offbynull.peernetic.core.actors.retry.StartRetryReceiveProxy;
import com.offbynull.peernetic.core.actors.retry.StartRetrySendProxy;
import com.offbynull.peernetic.core.actors.unreliable.SimpleLine;
import com.offbynull.peernetic.core.actors.unreliable.StartProxy;
import com.offbynull.peernetic.core.actors.unreliable.UnreliableProxyCoroutine;
import com.offbynull.peernetic.core.gateway.Gateway;
import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.gateways.timer.TimerGateway;
import com.offbynull.peernetic.core.gateways.udp.UdpGateway;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.lang3.Validate;

public class Test {

    public static void main(String[] args) throws InterruptedException {
//        basicTest();
//        basicTimer();
//        basicUdp();
//        basicUnreliable();
        basicRetry();
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
        
        echoerThread.addShuttle(senderThread.getShuttle());
        senderThread.addShuttle(echoerThread.getShuttle());
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
        ActorThread unreliableProxyThread = ActorThread.create("up");
        TimerGateway timerGateway = new TimerGateway("timer");
        
        unreliableProxyThread.addCoroutineActor("up", new UnreliableProxyCoroutine(),
                new StartProxy("timer", new SimpleLine(12345L), new SimpleSerializer()));
        unreliableProxyThread.addShuttle(timerGateway.getShuttle());
        unreliableProxyThread.addShuttle(echoerThread.getShuttle());
        unreliableProxyThread.addShuttle(senderThread.getShuttle());
        
        echoerThread.addShuttle(unreliableProxyThread.getShuttle());
        senderThread.addShuttle(unreliableProxyThread.getShuttle());
        timerGateway.addShuttle(unreliableProxyThread.getShuttle());
        
        echoerThread.addCoroutineActor("echoer", echoer);
        senderThread.addCoroutineActor("sender", sender, "up:up:echoer:echoer");
        
        latch.await();        
    }

    private static void basicUdp() throws InterruptedException {
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
        Shuttle echoerInputShuttle = echoerThread.getShuttle();
        Gateway echoerUdpGateway = new UdpGateway(new InetSocketAddress(1000), "internaludp", echoerInputShuttle, "echoer:echoer", new SimpleSerializer());
        Shuttle echoerOutputShuttle = echoerUdpGateway.getShuttle();
        
        ActorThread senderThread = ActorThread.create("sender");
        Shuttle senderInputShuttle = senderThread.getShuttle();
        Gateway senderUdpGateway = new UdpGateway(new InetSocketAddress(2000), "internaludp", senderInputShuttle, "sender:sender", new SimpleSerializer());
        Shuttle senderOutputShuttle = senderUdpGateway.getShuttle();
        
        echoerThread.addShuttle(echoerOutputShuttle);
        senderThread.addShuttle(senderOutputShuttle);
        
        echoerThread.addCoroutineActor("echoer", echoer);
        senderThread.addCoroutineActor("sender", sender, "internaludp:7f000001.1000");
        
        latch.await();        
    }

    private static void basicTimer() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        
        Coroutine tester = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            
            String timerPrefix = ctx.getIncomingMessage();
            ctx.addOutgoingMessage(timerPrefix + ":2000", 0);
            cnt.suspend();
            
            latch.countDown();
        };
        
        TimerGateway timerGateway = new TimerGateway("timer");
        Shuttle timerInputShuttle = timerGateway.getShuttle();
        
        ActorThread testerThread = ActorThread.create("local");
        Shuttle testerInputShuttle = testerThread.getShuttle();
        
        testerThread.addShuttle(timerInputShuttle);
        timerGateway.addShuttle(testerInputShuttle);
        
        testerThread.addCoroutineActor("tester", tester, "timer");
        
        latch.await();        
    }

    private static void basicRetry() throws InterruptedException {
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
        TimerGateway timerGateway = new TimerGateway("timer");

        
        senderThread.addShuttle(echoerThread.getShuttle());
        senderThread.addShuttle(timerGateway.getShuttle());
        
        echoerThread.addShuttle(senderThread.getShuttle());
        echoerThread.addShuttle(timerGateway.getShuttle());
        
        timerGateway.addShuttle(echoerThread.getShuttle());
        timerGateway.addShuttle(senderThread.getShuttle());
        
        IdExtractor idExtractor = x -> x.toString();
        ReceiveGuidelineGenerator recvGen = x -> new ReceiveGuideline(Duration.ofSeconds(1L));
        SendGuidelineGenerator sendGen = x -> new SendGuideline(Duration.ofSeconds(2L),
                Duration.ofMillis(50L),
                Duration.ofMillis(50L),
                Duration.ofMillis(50L),
                Duration.ofMillis(50L),
                Duration.ofMillis(50L),
                Duration.ofMillis(50L),
                Duration.ofMillis(50L),
                Duration.ofMillis(50L),
                Duration.ofMillis(50L),
                Duration.ofMillis(50L),
                Duration.ofMillis(50L));
        
        echoerThread.addCoroutineActor("echoer", echoer);
        echoerThread.addCoroutineActor("retryRecver", new RetryReceiveProxyCoroutine(),
                new StartRetryReceiveProxy("timer", "echoer:echoer", idExtractor, recvGen));
        
        senderThread.addCoroutineActor("sender", sender, "sender:retrySender");
        senderThread.addCoroutineActor("retrySender", new RetrySendProxyCoroutine(),
                new StartRetrySendProxy("timer", "echoer:retryRecver", idExtractor, sendGen));
        
        latch.await();        
    }
}
