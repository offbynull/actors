/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
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

import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import com.offbynull.peernetic.core.shuttles.simple.SimpleShuttle;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ActorRunnable implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ActorRunnable.class);

    private final String prefix;
    private final Bus bus;
    private final SimpleShuttle incomingShuttle;
    private final Runnable criticalFailureHandler;

    ActorRunnable(String prefix, Bus bus, Runnable criticalFailureHandler) {
        Validate.notNull(prefix);
        Validate.notNull(bus);
        Validate.notNull(criticalFailureHandler);
        Validate.notEmpty(prefix);

        this.prefix = prefix;
        this.bus = bus;
        this.incomingShuttle = new SimpleShuttle(prefix, bus);
        this.criticalFailureHandler = criticalFailureHandler;
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
                        Address src = incomingMessage.getSourceAddress();
                        Address dst = incomingMessage.getDestinationAddress();

                        processNormalMessage(msg, src, dst, actors, outgoingMessages);
                    } else {
                        processManagementMessage(incomingObject, actors, outgoingMessages, outgoingShuttles);
                    }
                }

                sendOutgoingMessages(outgoingMessages, outgoingShuttles);
            }
        } catch (InterruptedException ie) {
            LOG.debug("Actor thread interrupted");
            Thread.interrupted();
            
            // Invoke critical failure handler
            try {
                criticalFailureHandler.run();
            } catch (RuntimeException innerRe) {
                LOG.error("Handler failed", innerRe);
            } 
        } catch (RuntimeException re) {
            LOG.error("Internal error encountered", re);
            
            // Invoke critical failure handler
            try {
                criticalFailureHandler.run();
            } catch (RuntimeException innerRe) {
                LOG.error("Handler failed", innerRe);
            } 
        } finally {
            bus.close();
        }
    }

    private void processManagementMessage(Object msg, Map<String, LoadedActor> actors, List<Message> outgoingMessages,
            Map<String, Shuttle> outgoingShuttles) {
        LOG.debug("Processing management message: {}" , msg);
        if (msg instanceof AddActor) {
            AddActor aam = (AddActor) msg;
            
            Address self = Address.of(prefix, aam.getId());
            CoroutineRunner actorRunner = new CoroutineRunner(aam.getActor());
            SourceContext ctx = new SourceContext(actorRunner, self);
            
            actorRunner.setContext(ctx.toNormalContext());
            
            LoadedActor existingActor = actors.putIfAbsent(aam.getId(), new LoadedActor(ctx));
            
            Validate.isTrue(existingActor == null); // unable to add a actor with id that already exists
            
            List<Message> initialMessages = new LinkedList<>();
            for (Object primingMessage : aam.getPrimingMessages()) {
                Address dstAddr = Address.of(prefix, aam.getId());
                Message initialMessage = new Message(dstAddr, dstAddr, primingMessage);
                initialMessages.add(initialMessage);
            }
            outgoingMessages.addAll(initialMessages);
        } else if (msg instanceof RemoveActor) {
            RemoveActor ram = (RemoveActor) msg;
            LoadedActor existingActor = actors.remove(ram.getId());
            
            Validate.isTrue(existingActor != null); // unable to remove a actor that doesnt exist
        } else if (msg instanceof AddShuttle) {
            AddShuttle asm = (AddShuttle) msg;
            Shuttle existingShuttle = outgoingShuttles.putIfAbsent(asm.getShuttle().getPrefix(), asm.getShuttle());
            
            Validate.isTrue(existingShuttle == null); // unable to add a prefix for a shuttle that already exists
        } else if (msg instanceof RemoveShuttle) {
            RemoveShuttle rsm = (RemoveShuttle) msg;
            Shuttle existingShuttle = outgoingShuttles.remove(rsm.getPrefix());
            
            Validate.isTrue(existingShuttle != null); // unable to remove a shuttle prefix that doesnt exist
        } else {
            LOG.warn("No handler for management message: {}", msg);
        }
    }

    private void processNormalMessage(Object msg, Address src, Address dst, Map<String, LoadedActor> actors,
            List<Message> outgoingMessages) {
        // Get actor to dump to
        Validate.isTrue(dst.size() >= 2); // sanity check
        
        String dstPrefix = dst.getElement(0);
        String dstActorId = dst.getElement(1);
        Validate.isTrue(dstPrefix.equals(prefix)); // sanity check

        LoadedActor loadedActor = actors.get(dstActorId);
        if (loadedActor == null) {
            LOG.warn("Actor not found for message: id={} message={}", dst, msg);
            return;
        }

        SourceContext ctx = loadedActor.context;
        
        boolean shutdown = SourceContext.fire(ctx, src, dst, Instant.now(), msg);
        if (shutdown) {
            LOG.debug("Removing actor {}", dst);
            actors.remove(dstActorId);
        }

        // Queue up outgoing messages
        List<BatchedOutgoingMessage> batchedOutgoingMessages = ctx.copyAndClearOutgoingMessages();
        for (BatchedOutgoingMessage batchedOutgoingMessage : batchedOutgoingMessages) {
            Message outgoingMessage = new Message(
                    batchedOutgoingMessage.getSource(),
                    batchedOutgoingMessage.getDestination(),
                    batchedOutgoingMessage.getMessage());

            outgoingMessages.add(outgoingMessage);
        }
    }

    private void sendOutgoingMessages(List<Message> outgoingMessages, Map<String, Shuttle> outgoingShuttles) {
        // Group outgoing messages by prefix
        Map<String, List<Message>> outgoingMap = new HashMap<>();
        for (Message outgoingMessage : outgoingMessages) {
            Address outDst = outgoingMessage.getDestinationAddress();
            String outDstPrefix = outDst.getElement(0);

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
                // Log warning here saying shuttle doesn't exist
            }
        }
    }

    String getPrefix() {
        return prefix;
    }

    Shuttle getIncomingShuttle() {
        return incomingShuttle;
    }

    void addActor(String id, Coroutine coroutine, Object ... primingMessages) {
        Validate.notNull(id);
        Validate.notNull(coroutine);
        Validate.notNull(primingMessages);
        Validate.noNullElements(primingMessages);
        AddActor aam = new AddActor(id, coroutine, primingMessages);
        bus.add(aam);
    }

    void removeActor(String id) {
        Validate.notNull(id);
        RemoveActor ram = new RemoveActor(id);
        bus.add(ram);
    }

    void addOutgoingShuttle(Shuttle shuttle) {
        Validate.notNull(shuttle);
        Validate.notNull(shuttle.getPrefix()); // sanity check
        AddShuttle asm = new AddShuttle(shuttle);
        bus.add(asm);
    }

    void removeOutgoingShuttle(String prefix) {
        Validate.notNull(prefix);
        RemoveShuttle rsm = new RemoveShuttle(prefix);
        bus.add(rsm);
    }
    
    private static final class LoadedActor {
        private final SourceContext context;

        public LoadedActor(SourceContext context) {
            Validate.notNull(context);
            this.context = context;
        }
    }
}
