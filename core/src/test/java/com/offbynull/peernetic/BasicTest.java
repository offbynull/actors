package com.offbynull.peernetic;

import com.offbynull.peernetic.actor.ActorRunnable;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.fsm.FiniteStateMachine;
import com.offbynull.peernetic.fsm.StateHandler;
import com.offbynull.peernetic.network.UdpGateway;
import com.offbynull.peernetic.network.handlers.XStreamDecodeHandler;
import com.offbynull.peernetic.network.handlers.XStreamEncodeHandler;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import org.junit.Assert;
import org.junit.Test;

public class BasicTest {

    public BasicTest() {
    }

    @Test
    public void testBasic() throws Throwable {
        Sender sender = new Sender();
        FsmActor fsmActor1 = new FsmActor(sender, Sender.START_STATE);
        ActorRunnable runnable1 = ActorRunnable.createAndStart(fsmActor1);
        GatewayInputAdapter inputAdapter1 = new GatewayInputAdapter(runnable1.getEndpoint(fsmActor1));
        InetSocketAddress address1 = new InetSocketAddress(InetAddress.getLocalHost(), 9000);
        UdpGateway udpGateway1 = new UdpGateway(
                address1,
                inputAdapter1,
                new XStreamEncodeHandler(), new XStreamDecodeHandler());

        Recver recver = new Recver();
        FsmActor fsmActor2 = new FsmActor(recver, Recver.RESPOND_STATE);
        ActorRunnable runnable2 = ActorRunnable.createAndStart(fsmActor2);
        GatewayInputAdapter inputAdapter2 = new GatewayInputAdapter(runnable2.getEndpoint(fsmActor2));
        InetSocketAddress address2 = new InetSocketAddress(InetAddress.getLocalHost(), 9001);
        UdpGateway udpGateway2 = new UdpGateway(
                address2,
                inputAdapter2,
                new XStreamEncodeHandler(), new XStreamDecodeHandler());
        
        
        GatewayOutputEndpoint from1To2Endpoint = new GatewayOutputEndpoint(udpGateway1, address2);
        runnable1.getEndpoint(fsmActor1).send(NullEndpoint.INSTANCE, new Init(from1To2Endpoint));
        
        sender.awaitCompletion();
        
        udpGateway1.close();
        udpGateway2.close();
        runnable1.shutdown();
        runnable2.shutdown();
    }

    private static final class Sender {

        public static final String START_STATE = "START";
        public static final String SEND_STATE = "SEND";
        public static final String WAIT_STATE = "WAIT";

        private Endpoint destination;
        private CountDownLatch counter = new CountDownLatch(30);

        @StateHandler(START_STATE)
        public void handleInitial(String state, FiniteStateMachine fsm, Instant instant, Init message, Endpoint source) {
            destination = message.destination;

            fsm.switchStateAndProcess(SEND_STATE, instant, message, source);
        }

        @StateHandler(SEND_STATE)
        public void handleSend(String state, FiniteStateMachine fsm, Instant instant, Object message, Endpoint source) {
            if (counter.getCount() > 0) {
                counter.countDown();
                destination.send(source, counter.getCount());
                
                fsm.setState(WAIT_STATE);
            }
        }

        @StateHandler(WAIT_STATE)
        public void handleWait(String state, FiniteStateMachine fsm, Instant instant, Object message, Endpoint source) {
            Assert.assertEquals(1000L + counter.getCount(), message);
            fsm.switchStateAndProcess(SEND_STATE, instant, message, source);
        }
        
        public void awaitCompletion() throws InterruptedException {
            counter.await();
        }
    }

    private static final class Recver {

        public static final String RESPOND_STATE = "RESPOND";

        @StateHandler(RESPOND_STATE)
        public void handleRespond(String state, FiniteStateMachine fsm, Instant instant, Long message, Endpoint source) {
            source.send(NullEndpoint.INSTANCE, 1000L + message);
        }
    }

    private static final class Init {

        private final Endpoint destination;

        public Init(Endpoint destination) {
            this.destination = destination;
        }
        
    }
}
