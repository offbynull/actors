package com.offbynull.peernetic.debug.testnetwork;

import com.offbynull.peernetic.debug.actornetwork.Hub;
import com.offbynull.peernetic.debug.actornetwork.HubEndpointDirectory;
import com.offbynull.peernetic.debug.actornetwork.SimpleLine;
import com.offbynull.peernetic.FsmActor;
import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.ActorRunnable;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.actor.SimpleEndpointScheduler;
import com.offbynull.peernetic.debug.actornetwork.messages.JoinHub;
import com.offbynull.peernetic.debug.actornetwork.messages.LeaveHub;
import com.offbynull.peernetic.debug.actornetwork.messages.StartHub;
import com.offbynull.peernetic.network.XStreamSerializer;
import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Assert;
import org.junit.Test;

public final class BasicHubTest {

    @Test
    public void basicHubTest() throws InterruptedException {
        EndpointScheduler endpointScheduler = new SimpleEndpointScheduler();
        
        // Create hub
        Hub<String> hub = new Hub<>();
        FsmActor hubFsmActor = FsmActor.create(hub, Hub.INITIAL_STATE);
        ActorRunnable hubActorRunnable = ActorRunnable.createAndStart(hubFsmActor);
        Endpoint hubEndpoint = hubActorRunnable.getEndpoint(hubFsmActor);
        
        // Create actors
        AtomicReference<EndpointDirectory<String>> directoryForSenderRef = new AtomicReference<>();
        AtomicReference<EndpointDirectory<String>> directoryForRecverRef = new AtomicReference<>();
        SendActor senderActor = new SendActor(directoryForSenderRef, "sender", "recver");
        RespondActor recverActor = new RespondActor(directoryForRecverRef, "recver");
        ActorRunnable actorRunnable = ActorRunnable.createAndStart(senderActor, recverActor);
        
        Endpoint senderEndpoint = actorRunnable.getEndpoint(senderActor);
        Endpoint recverEndpoint = actorRunnable.getEndpoint(recverActor);
        
        EndpointDirectory<String> directoryForSender = new HubEndpointDirectory<>("sender", hubEndpoint);
        EndpointDirectory<String> directoryForRecver = new HubEndpointDirectory<>("recver", hubEndpoint);
        directoryForSenderRef.set(directoryForSender);
        directoryForRecverRef.set(directoryForRecver);
        
        // Have actors join hubs
        hubEndpoint.send(NullEndpoint.INSTANCE, new StartHub<>(endpointScheduler, new SimpleLine(0), new XStreamSerializer(), hubEndpoint));
        hubEndpoint.send(senderEndpoint, new JoinHub<>("sender"));
        hubEndpoint.send(recverEndpoint, new JoinHub<>("recver"));
        
        // Send a message to sender instruct it to send -- sender should send a msg to recver, and recver should respond back to sender
          // sender holds on the original send message we trigger it with + recver's response
        senderEndpoint.send(NullEndpoint.INSTANCE, "send");
        Thread.sleep(1000L);
        Assert.assertArrayEquals(new Object[] { "send", "resphi" }, senderActor.getMessages());
        
        // Have recver leave hub
        hubEndpoint.send(recverEndpoint, new LeaveHub<>("recver"));
        
        // Send a message to sender instruct it to send -- this time recver should not respond at all
        senderActor.clearMessages();
        
        senderEndpoint.send(NullEndpoint.INSTANCE, "send");
        Thread.sleep(1000L);
        Assert.assertArrayEquals(new Object[] { "send", }, senderActor.getMessages());
        
        
        // Shutdown
        hubActorRunnable.shutdown();
        actorRunnable.shutdown();
    }
    
    private static final class SendActor implements Actor {

        private ConcurrentLinkedQueue<Object> messages;
        private AtomicReference<EndpointDirectory<String>> directory;
        private String self;
        private String who;

        public SendActor(AtomicReference<EndpointDirectory<String>> directory, String self, String who) {
            this.messages = new ConcurrentLinkedQueue<>();
            this.directory = directory;
            this.self = self;
            this.who = who;
        }

        public Object[] getMessages() {
            return messages.toArray();
        }

        public void clearMessages() {
            messages.clear();
        }
        
        @Override
        public void onStart(Instant time) throws Exception {
        }

        @Override
        public void onStep(Instant time, Endpoint source, Object message) throws Exception {
            messages.add(message);
            
            Endpoint dst = directory.get().lookup(who);
            Endpoint src = directory.get().lookup(self);
            
            if (message.equals("send")) {
                dst.send(src, "hi");
            }
        }

        @Override
        public void onStop(Instant time) throws Exception {
        }
        
    }
    
    
    private static final class RespondActor implements Actor {

        private final String self;
        private final AtomicReference<EndpointDirectory<String>> directory;

        public RespondActor(AtomicReference<EndpointDirectory<String>> directory, String self) {
            this.self = self;
            this.directory = directory;
        }
        
        @Override
        public void onStart(Instant time) throws Exception {
        }

        @Override
        public void onStep(Instant time, Endpoint source, Object message) throws Exception {
            Endpoint selfEndpoint = directory.get().lookup(self);
            source.send(selfEndpoint, "resp" + message);
        }

        @Override
        public void onStop(Instant time) throws Exception {
        }
        
    }
}
