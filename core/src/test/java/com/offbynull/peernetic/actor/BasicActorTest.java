package com.offbynull.peernetic.actor;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;
import com.offbynull.peernetic.CoroutineActor;
import com.offbynull.peernetic.CoroutineActor.Context;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.lang3.Validate;
import org.junit.Test;

public final class BasicActorTest {

    private static final int COUNT = 10;

    @Test
    public void basicActorsTest() throws Throwable {
//        final CountDownLatch latch = new CountDownLatch(10);
//        Coroutine coroutine = new Coroutine() {
//
//            @Override
//            public void run(Continuation c) throws Exception {
//                System.out.println("1");
//                Context context = (Context) c.getContext();
//
//                Endpoint selfEp = (Endpoint) context.getMessage();
//                c.suspend();
//                System.out.println("2");
//                Endpoint dstEp = (Endpoint) context.getMessage();
//
//                for (int i = 0; i < 10; i++) {
//                    dstEp.send(selfEp, i);
//                    System.out.println("3");
//                    c.suspend();
//                    System.out.println("i currently is " + i);
////                Validate.isTrue(context.getMessage().equals(i));
////                try {
////                    latch.countDown();
////                } catch (Exception e) {
////                    System.out.println("lolwat?");
////                }
////                System.out.println("5");
//                }
//            }
//        };
//        
//        Context context = new Context();
//        CoroutineRunner runner = new CoroutineRunner(coroutine);
//        runner.setContext(context);
//
//        context.setSource(NullEndpoint.INSTANCE);
//
//        context.setMessage(NullEndpoint.INSTANCE);
//        runner.execute();
//        context.setMessage(NullEndpoint.INSTANCE);
//        runner.execute();
//
//        context.setMessage(0);
//        runner.execute();
//        context.setMessage(1);
//        runner.execute();
//        context.setMessage(2);
//        runner.execute();
//        context.setMessage(3);
//        runner.execute();

        final CountDownLatch latch = new CountDownLatch(10);
        
        Actor sender = new CoroutineActor(c -> {
            System.out.println("1");
            Context context = (Context) c.getContext();
            
            Endpoint selfEp = (Endpoint) context.getMessage();
            c.suspend();
            System.out.println("2");
            Endpoint dstEp = (Endpoint) context.getMessage();
            
            for (int i = 0; i < 10; i++) {
                dstEp.send(selfEp, i);
                System.out.println("3");
                c.suspend();
                System.out.println("4");
                Validate.isTrue(context.getMessage().equals(i));
                try {
                    latch.countDown();
                } catch (Exception e) {
                    System.out.println("lolwat?");
                }
                System.out.println("5");
            }
        });
        
        Actor echoer = new CoroutineActor(c -> {
            Context context = (Context) c.getContext();
            
            Endpoint selfEp = (Endpoint) context.getMessage();
            
            while (true) {
                c.suspend();
                context.getSource().send(selfEp, context.getMessage());
            }
        });

        ActorRunnable actorRunnable = ActorRunnable.createAndStart(sender, echoer);
        Endpoint senderEp = actorRunnable.getEndpoint(sender);
        Endpoint echoerEp = actorRunnable.getEndpoint(echoer);
        
        senderEp.send(NullEndpoint.INSTANCE, senderEp, echoerEp);
        echoerEp.send(NullEndpoint.INSTANCE, echoerEp);
        
        latch.await();

        actorRunnable.shutdown();
    }
}
