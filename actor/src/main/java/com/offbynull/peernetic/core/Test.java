package com.offbynull.peernetic.core;

import com.offbynull.peernetic.core.actor.ActorRunnable;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.core.gateway.Gateway;
import com.offbynull.peernetic.core.gateway.SimpleSerializer;
import com.offbynull.peernetic.core.gateways.udp.UdpGateway;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.lang3.Validate;

public class Test {

    public static void main(String[] args) throws InterruptedException {
        basicUdp();
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
        
        ActorThread echoerThread = ActorRunnable.create("echoer");
        ActorRunnable echoerActorRunnable = echoerThread.getActorRunnable();
        
        ActorThread senderThread = ActorRunnable.create("sender");
        ActorRunnable senderActorRunnable = senderThread.getActorRunnable();
        
        echoerActorRunnable.addShuttle(senderActorRunnable.getShuttle());
        senderActorRunnable.addShuttle(echoerActorRunnable.getShuttle());
        echoerActorRunnable.addCoroutineActor("echoer", echoer);
        senderActorRunnable.addCoroutineActor("sender", sender, "echoer:echoer");
        
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
        
        ActorThread echoerThread = ActorRunnable.create("echoer");
        ActorRunnable echoerActorRunnable = echoerThread.getActorRunnable();
        Shuttle echoerInputShuttle = echoerThread.getShuttle();
        Gateway echoerUdpGateway = new UdpGateway(new InetSocketAddress(1000), "internaludp", echoerInputShuttle, "echoer:echoer", new SimpleSerializer());
        Shuttle echoerOutputShuttle = echoerUdpGateway.getShuttle();
        
        ActorThread senderThread = ActorRunnable.create("sender");
        ActorRunnable senderActorRunnable = senderThread.getActorRunnable();
        Shuttle senderInputShuttle = senderThread.getShuttle();
        Gateway senderUdpGateway = new UdpGateway(new InetSocketAddress(2000), "internaludp", senderInputShuttle, "sender:sender", new SimpleSerializer());
        Shuttle senderOutputShuttle = senderUdpGateway.getShuttle();
        
        echoerActorRunnable.addShuttle(echoerOutputShuttle);
        senderActorRunnable.addShuttle(senderOutputShuttle);
        
        echoerActorRunnable.addCoroutineActor("echoer", echoer);
        senderActorRunnable.addCoroutineActor("sender", sender, "internaludp:7f000001.1000");
        
        latch.await();        
    }

}
