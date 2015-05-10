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
package com.offbynull.peernetic.core.actor;

import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.coroutines.user.Coroutine;
import static com.offbynull.peernetic.core.shuttle.AddressUtils.getAddress;
import static com.offbynull.peernetic.core.shuttle.AddressUtils.SEPARATOR;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import com.offbynull.peernetic.core.shuttles.simple.SimpleShuttle;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.collections4.collection.UnmodifiableCollection;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ActorRunnable implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActorRunnable.class);

    private final String prefix;
    private final Bus bus;
    private final SimpleShuttle incomingShuttle;

    ActorRunnable(String prefix, Bus bus) {
        Validate.notNull(prefix);
        Validate.notNull(bus);
        Validate.notEmpty(prefix);

        this.prefix = prefix;
        this.bus = bus;
        incomingShuttle = new SimpleShuttle(prefix, bus);
    }

    @Override
    public void run() {
        try {
            Map<String, Shuttle> outgoingShuttles = new HashMap<>(); // prefix -> shuttle
            Map<String, LoadedActor> actors = new HashMap<>(); // id -> actor

            while (true) {
                List<Object> incomingObjects = bus.pull();
                List<Message> outgoingMessages = new LinkedList<>(); // outgoing messages destined for destinations not in here

                for (Object incomingObject : incomingObjects) {
                    if (incomingObject instanceof Message) {
                        Message incomingMessage = (Message) incomingObject;
                        
                        Object msg = incomingMessage.getMessage();
                        String src = incomingMessage.getSourceAddress();
                        String dst = incomingMessage.getDestinationAddress();

                        processNormalMessage(msg, src, dst, actors, outgoingMessages);
                    } else {
                        processManagementMessage(incomingObject, actors, outgoingMessages, outgoingShuttles);
                    }
                }

                sendOutgoingMessages(outgoingMessages, outgoingShuttles);
            }
        } catch (InterruptedException ie) {
            //throw new RuntimeException(ie); // do nothing
            Thread.interrupted();
        } finally {
            bus.close();
        }
    }

    private void processManagementMessage(Object msg, Map<String, LoadedActor> actors, List<Message> outgoingMessages,
            Map<String, Shuttle> outgoingShuttles) {
        if (msg instanceof AddActorMessage) {
            AddActorMessage aam = (AddActorMessage) msg;
            actors.put(aam.id, new LoadedActor(aam.actor, new SourceContext()));
            List<Message> initialMessages = new LinkedList<>();
            for (Object primingMessage : aam.primingMessages) {
                String dstAddr = getAddress(prefix, aam.id);
                Message initialMessage = new Message(dstAddr, dstAddr, primingMessage);
                initialMessages.add(initialMessage);
            }
            outgoingMessages.addAll(initialMessages);
        } else if (msg instanceof RemoveActorMessage) {
            RemoveActorMessage ram = (RemoveActorMessage) msg;
            actors.remove(ram.id);
        } else if (msg instanceof AddShuttleMessage) {
            AddShuttleMessage asm = (AddShuttleMessage) msg;
            Shuttle existingShuttle = outgoingShuttles.putIfAbsent(asm.shuttle.getPrefix(), asm.shuttle);
            
            Validate.isTrue(existingShuttle == null); // unable to add a prefix for a shuttle that already exists
        } else if (msg instanceof RemoveShuttleMessage) {
            RemoveShuttleMessage rsm = (RemoveShuttleMessage) msg;
            Shuttle existingShuttle = outgoingShuttles.remove(rsm.prefix);
            
            Validate.isTrue(existingShuttle != null); // unable to remove a shuttle prefix that doesnt exist
        } else {
            LOGGER.warn("Unprocessed management message: {}", msg);
        }
    }

    private void processNormalMessage(Object msg, String src, String dst, Map<String, LoadedActor> actors, List<Message> outgoingMessages) {
        // Get actor to dump to
        String[] splitDst = AddressUtils.splitAddress(dst);
        Validate.isTrue(splitDst.length >= 2); // sanity check
        
        String dstPrefix = splitDst[0];
        String dstImmediateId = splitDst[1];
        Validate.isTrue(dstPrefix.equals(prefix)); // sanity check

        LoadedActor loadedActor = actors.get(dstImmediateId);
        if (loadedActor == null) {
            LOGGER.warn("Undeliverable message: id={} message={}", dstImmediateId, msg);
            return;
        }

        Actor actor = loadedActor.actor;
        SourceContext context = loadedActor.context;
        context.setSelf(dstPrefix + SEPARATOR + dstImmediateId);
        context.setIncomingMessage(msg);
        context.setSource(src);
        context.setDestination(dst);
        context.setTime(Instant.now());

        // Pass to actor, shut down if returns false or throws exception
        boolean shutdown = false;
        try {
            if (!actor.onStep(context.toNormalContext())) {
                shutdown = true;
            }
        } catch (Exception e) {
            LOGGER.error("Actor " + dst + " threw an exception", e);
            shutdown = true;
        }

        if (shutdown) {
            actors.remove(dstImmediateId);
        }

        // Queue up outgoing messages
        List<BatchedOutgoingMessage> batchedOutgoingMessages = context.copyAndClearOutgoingMessages();
        for (BatchedOutgoingMessage batchedOutgoingMessage : batchedOutgoingMessages) {
            String sentFrom;
            String srcId = batchedOutgoingMessage.getSourceId();
            sentFrom = dstPrefix + SEPARATOR + dstImmediateId + (srcId != null ? SEPARATOR + srcId : "");
            
            Message outgoingMessage = new Message(
                    sentFrom,
                    batchedOutgoingMessage.getDestination(),
                    batchedOutgoingMessage.getMessage());

            outgoingMessages.add(outgoingMessage);
        }
    }

    private void sendOutgoingMessages(List<Message> outgoingMessages, Map<String, Shuttle> outgoingShuttles) {
        // Group outgoing messages by prefix
        Map<String, List<Message>> outgoingMap = new HashMap<>();
        for (Message outgoingMessage : outgoingMessages) {
            String outDst = outgoingMessage.getDestinationAddress();
            String outDstPrefix = AddressUtils.getElement(outDst, 0);

            List<Message> batchedMessages = outgoingMap.get(outDstPrefix);
            if (batchedMessages == null) {
                batchedMessages = new LinkedList<>();
                outgoingMap.put(outDstPrefix, batchedMessages);
            }

            batchedMessages.add(outgoingMessage);
        }

        // Send outgoing messaged by prefix
        for (Entry<String, List<Message>> entry : outgoingMap.entrySet()) {
            Shuttle shuttle = outgoingShuttles.get(entry.getKey());
            if (shuttle != null) {
                shuttle.send(entry.getValue());
            } else {
                // TODO: Log warning here saying shuttle doesn't exist
            }
        }
    }

    String getPrefix() {
        return prefix;
    }

    Shuttle getIncomingShuttle() {
        return incomingShuttle;
    }
    
    void addActor(String id, Actor actor, Object ... primingMessages) {
        Validate.notNull(id);
        Validate.notNull(actor);
        Validate.notNull(primingMessages);
        Validate.noNullElements(primingMessages);
        AddActorMessage aam = new AddActorMessage(id, actor, primingMessages);
        bus.add(aam);
    }

    void addCoroutineActor(String id, Coroutine coroutine, Object ... primingMessages) {
        Validate.notNull(id);
        Validate.notNull(coroutine);
        Validate.notNull(primingMessages);
        Validate.noNullElements(primingMessages);
        AddActorMessage aam = new AddActorMessage(id, new CoroutineActor(coroutine), primingMessages);
        bus.add(aam);
    }

    void removeActor(String id) {
        Validate.notNull(id);
        RemoveActorMessage ram = new RemoveActorMessage(id);
        bus.add(ram);
    }

    void addOutgoingShuttle(Shuttle shuttle) {
        Validate.notNull(shuttle);
        AddShuttleMessage asm = new AddShuttleMessage(shuttle);
        bus.add(asm);
    }

    void removeOutgoingShuttle(String prefix) {
        Validate.notNull(prefix);
        RemoveShuttleMessage rsm = new RemoveShuttleMessage(prefix);
        bus.add(rsm);
    }
    
    private static final class LoadedActor {
        private final Actor actor;
        private final SourceContext context;

        public LoadedActor(Actor actor, SourceContext context) {
            Validate.notNull(actor);
            Validate.notNull(context);
            this.actor = actor;
            this.context = context;
        }
    }
    

    static final class AddActorMessage {

        private final String id;
        private final Actor actor;
        private final UnmodifiableCollection<Object> primingMessages;

        public AddActorMessage(String id, Actor actor, Object... primingMessages) {
            Validate.notNull(id);
            Validate.notNull(actor);
            Validate.notNull(primingMessages);
            Validate.noNullElements(primingMessages);

            this.id = id;
            this.actor = actor;
            this.primingMessages = (UnmodifiableCollection<Object>) UnmodifiableCollection.<Object>unmodifiableCollection(
                    Arrays.asList(primingMessages));
        }
    }

    static final class RemoveActorMessage {

        private final String id;

        public RemoveActorMessage(String id) {
            Validate.notNull(id);

            this.id = id;
        }
    }

    static final class AddShuttleMessage {

        private final Shuttle shuttle;

        public AddShuttleMessage(Shuttle shuttle) {
            Validate.notNull(shuttle);
            this.shuttle = shuttle;
        }
    }

    static final class RemoveShuttleMessage {

        private final String prefix;

        public RemoveShuttleMessage(String prefix) {
            Validate.notNull(prefix);

            this.prefix = prefix;
        }
    }
}
