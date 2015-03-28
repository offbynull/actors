package com.offbynull.peernetic.actor;

import com.offbynull.coroutines.user.Coroutine;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.lang3.Validate;

public class Test {

    public static void main(String[] args) throws InterruptedException {
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
        
        ActorRunnable.ActorThread echoerThread = ActorRunnable.create("echoer");
        ActorRunnable.ActorThread senderThread = ActorRunnable.create("sender");
        
        ActorRunnable echoerActorRunnable = echoerThread.getActorRunnable();
        ActorRunnable senderActorRunnable = senderThread.getActorRunnable();
        
        echoerActorRunnable.addShuttle(senderActorRunnable.getShuttle());
        senderActorRunnable.addShuttle(echoerActorRunnable.getShuttle());
        echoerActorRunnable.addCoroutineActor("echoer", echoer);
        senderActorRunnable.addCoroutineActor("sender", sender, "echoer:echoer");
        
        latch.await();
    }

}
