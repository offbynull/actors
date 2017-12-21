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
package com.offbynull.actors.gateways.actor;

import com.offbynull.actors.gateways.actor.Context.BatchedCreateChildCommand;
import com.offbynull.actors.gateways.actor.Context.BatchedCreateRootCommand;
import com.offbynull.actors.gateways.actor.Context.BatchedOutgoingMessageCommand;
import com.offbynull.actors.gateways.actor.Context.ShortcircuitLogic;
import static com.offbynull.actors.gateways.actor.Context.SuspendFlag.FORWARD_AND_RELEASE;
import static com.offbynull.actors.gateways.actor.Context.SuspendFlag.FORWARD_AND_RETURN;
import static com.offbynull.actors.gateways.actor.Context.SuspendFlag.RELEASE;
import static com.offbynull.actors.gateways.actor.SerializableActor.deserialize;
import static com.offbynull.actors.gateways.actor.SerializableActor.serialize;
import com.offbynull.actors.gateways.actor.Store.StoredWork;
import com.offbynull.actors.shuttle.Shuttle;
import com.offbynull.actors.shuttle.Message;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.actors.address.Address;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.offbynull.coroutines.user.CoroutineRunner;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

final class ActorRunnable implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ActorRunnable.class);

    private final String prefix;
    private final ConcurrentHashMap<String, Shuttle> outShuttles;
    private final Store store;
    
    private final FailListener failListener;
    private final AtomicBoolean shutdownFlag;

    ActorRunnable(
            String prefix,
            ConcurrentHashMap<String, Shuttle> outShuttles,
            Store store,
            FailListener failListener,
            AtomicBoolean shutdownFlag) {
        Validate.notNull(prefix);
        Validate.notNull(outShuttles);
        Validate.notNull(store);
        Validate.notNull(failListener);
        Validate.notNull(shutdownFlag);
        // DONT CHECK outShuttles FOR NULL keys/values as there's no point -- map is concurrent, being modified by other threads
        Validate.notEmpty(prefix);

        this.prefix = prefix;
        this.outShuttles = outShuttles;
        this.store = store;
        this.failListener = failListener;
        this.shutdownFlag = shutdownFlag;
    }

    @Override
    public void run() {
        while (!shutdownFlag.get()) {
            try {
                processWork();
            } catch (RuntimeException re) {
                LOG.error("Internal error encountered", re);

                try {
                    failListener.failed(re);
                } catch (RuntimeException innerRe) {
                    LOG.error("Handler failed", innerRe);
                }
            }
        }
    }

    private void processWork() {
        StoredWork work = store.take();

        Message message = work.getMessage();
        SerializableActor serializableActor = work.getActor();

        Object payload = message.getMessage();
        Address src = message.getSourceAddress();
        Address dst = message.getDestinationAddress();



        // deserialie
        Actor actor = deserialize(serializableActor);

        // reset checkpoint updated flag (user will set it again if they want to checkpoint)
        actor.context().checkpointUpdated(false);
        
        // fire msg
        boolean shutdown = fire(actor, src, dst, Instant.now(), payload);
        Context ctx = actor.context();


        
        List<Message> newOutgoingMessages = new LinkedList<>();
        List<SerializableActor> newRootActors = new LinkedList<>();
        
        // create children BEFORE attempting to store -- children are bundled as part of the main actor
        //   priming messages for children don't get sent until below
        List<BatchedCreateChildCommand> newChildCommands = ctx.copyAndClearNewChildren();
        createChildren(actor, newChildCommands, newOutgoingMessages);
        
        // create new actors (does not actually store until below)
        List<BatchedCreateRootCommand> newRootCommands = ctx.copyAndClearNewRoots();
        createActors(newRootCommands, newOutgoingMessages, newRootActors);
        
        // create outgoing messages (does not actually store until below)
        List<BatchedOutgoingMessageCommand> newMessageCommands = ctx.copyAndClearOutgoingMessages();
        createMessages(newMessageCommands, newOutgoingMessages);
        
        
        if (shutdown) {
            Address actorAddr = actor.context().self();
            LOG.debug("Actor shut down {} -- removing from storage engine", actorAddr);
            store.discard(actorAddr);
            
            // Actor has finished, store new actors+messages BUT discard messages going to self. Self has been discarded so the messages
            // will end up going nowhere anyways.
            newOutgoingMessages = newOutgoingMessages.stream()
                    .filter(m -> !ctx.self().isPrefixOf(m.getDestinationAddress()))
                    .collect(toList());
        } else {
            serializableActor = serialize(actor);
            boolean stored = store.store(serializableActor);
            if (!stored) {
                // Actor couldn't be stored because it was superseded by a checkpoint (took too long to execute a msg?). This instance of
                // the actor is no longer valid. Whatever it computed in terms of new messages to send out or new root actors to spawn
                // should be discarded.
                newRootActors.clear();
                newOutgoingMessages.clear();
            }
        }

        storeActors(newRootActors);
        storeMessages(ctx.self(), newOutgoingMessages);
    }

    private void createMessages(List<BatchedOutgoingMessageCommand> commands, List<Message> newMessages) {
        commands.stream()
                .map(m -> new Message(m.getSource(), m.getDestination(), m.getMessage()))
                .forEach(newMessages::add);
    }

    private void createActors(List<BatchedCreateRootCommand> commands, List<Message> newMessages, List<SerializableActor> newActors) {
        commands.stream().forEach(bcac -> {
            String newId = bcac.getId();
            Coroutine newCoroutine = bcac.getCoroutine();
            Address newSelf = Address.of(prefix, newId);

            Context newCtx = new Context(newSelf);

            CoroutineRunner newRunner = new CoroutineRunner(newCoroutine);
            newRunner.setContext(newCtx);
        


            Actor actor = new Actor(null, newRunner, newCtx);
            
            SerializableActor serializableActor = serialize(actor);
            newActors.add(serializableActor);
            
            bcac.getPrimingMessages().stream()
                    .map(payload -> new Message(newCtx.self(), newCtx.self(), payload))
                    .forEach(newMessages::add);
        });
    }

    private void createChildren(Actor rootActor, List<BatchedCreateChildCommand> commands, List<Message> newMessages) {
        commands.stream().forEach(bccc -> {
            Context parentContext = bccc.fromContext();
            String childId = bccc.getId();
            Coroutine childCoroutine = bccc.getCoroutine();

            Context childCtx = new Context(parentContext, childId);
            Address childSelf = childCtx.self();
            CoroutineRunner childRunner = new CoroutineRunner(childCoroutine);
            childRunner.setContext(childCtx);


            Actor parentActor = findActorForContext(rootActor, parentContext);
            Validate.validState(parentActor != null); // sanity test -- should never happen

            Actor childActor = new Actor(parentActor, childRunner, childCtx);
            parentActor.children().put(childId, childActor);


            bccc.getPrimingMessages().stream()
                    .map(payload -> new Message(childCtx.self(), childCtx.self(), payload))
                    .forEach(newMessages::add);
        });
    }
    
    private void storeMessages(Address self, List<Message> newMessages) {
        // Group outgoing messages by prefix
        Map<String, List<Message>> outgoingMap = newMessages.stream()
                .collect(groupingBy(x -> x.getDestinationAddress().getElement(0)));

        // Send outgoing messages for THIS prefix (storage messages)
        List<Message> selfMessages = outgoingMap.remove(prefix);
        if (selfMessages != null) {
            store.store(selfMessages);
        }
        
        // Send outgoing messages for other prefixes
        outgoingMap.entrySet().stream().forEach(e -> {
            String outgoingPrefix = e.getKey();

            Shuttle shuttle = outShuttles.get(outgoingPrefix);
            if (shuttle == null) { // maybe do a stream filter for this
                // LOG error?
                return;
            }

            List<Message> bundle = e.getValue();
            shuttle.send(bundle);
        });
    }

    private void storeActors(List<SerializableActor> newActors) {
        newActors.forEach(x -> {
            boolean stored = store.store(x);
            if (!stored) {
                LOG.warn("Unable to write newly spawned actor {}", x.getSelf());
            }
        });
    }

    private Actor findActorForContext(Actor actor, Context ctx) {
        if (actor.context() == ctx) {
            return actor;
        }
        
        for (Actor child : actor.children().values()) {
            Actor found = findActorForContext(child, ctx);
            if (found != null) {
                return found;
            }
        }
        
        return null;
    }
    
    
    
    
    
    
    
    // Fire a message to the actor associated with this context. If the destination is a child actor, the message will be correctly routed.
    private static boolean fire(Actor actor, Address src, Address dst, Instant time, Object payload) {
        Validate.notNull(actor);
        Validate.notNull(src);
        Validate.notNull(dst);
        Validate.notNull(time);
        Validate.notNull(payload);


        // Walk up the context chain until you reach the topmost actor
        while (actor.parent() != null) {
            actor = actor.parent();
        }
        
        return fireRecurse(actor, src, dst, time, payload);
    }
    
    private static boolean fireRecurse(Actor actor, Address src, Address dst, Instant time, Object payload) {
        if (actor == null) {
            return false;
        }
        
        Context ctx = actor.context();
        
        if (ctx.self().equals(dst)) {
            // The message is for us. There's no further child to recurse to so process and get out.
            ctx.mode(RELEASE);
            boolean done = invoke(actor, src, dst, time, payload);
            return done;
        }
        
        
        
        
        if (ctx.intercept()) {
            // The message is for one of our children, but we want to intercept it....
            boolean done = invoke(actor, src, dst, time, payload);
            if (done) {
                ctx.mode(RELEASE);
                return true; // we are the main actor and we died/finished OR we are a child actor that had an error, so return
            }

            if (ctx.mode() == RELEASE) {
                ctx.mode(RELEASE);
                return false; // we gave instructions NOT to forward, so return
            }
        }
        
        
        
        // Recurse down 1 level
        String childId = dst.removePrefix(ctx.self()).getElement(0);
        Actor childActor = actor.getChild(childId);
        if (childActor != null) {
            fireRecurse(childActor, src, dst, time, payload);
        }

        
        
        
        if (ctx.intercept() && ctx.mode() == FORWARD_AND_RETURN) {
            // If we intercepted this message and forwarded it + asked control to be returned back to the forwarder
            boolean done = invoke(actor, src, dst, time, payload);
            if (done) {
                ctx.mode(RELEASE);
                return true;
            }
            
            // If were instructed to forward to children at this point, something is wrong with the actor logic. We're releasing control
            // back to the actor from a forward for cleanup purposes. It makes zero sense to try to forward again. As such, kill the entire
            // actor stream
            if (ctx.mode() == FORWARD_AND_RETURN || ctx.mode() == FORWARD_AND_RELEASE) {
                LOG.error("Actor " + dst + " is instructing to forward on release from a forward -- not allowed");
                ctx.mode(RELEASE);
                return true;
            }
        }

        
        
        // Return okay status
        ctx.mode(RELEASE);
        return false;
    }
    
    private static boolean invoke(Actor actor, Address src, Address dst, Instant time, Object payload) {
        Context ctx = actor.context();
        if (ctx.ruleSet().evaluate(src, payload.getClass()) != RuleSet.AccessType.ALLOW) {
            LOG.warn("Actor ruleset rejected message: id={} message={}", dst, payload);
            return false;
        }

        
        LOG.debug("Processing message from {} to {} {}", src, dst, payload);
        
        ctx.in(payload);
        ctx.source(src);
        ctx.destination(dst);
        ctx.time(time);
        
        try {
            ShortcircuitLogic shortcircuit = ctx.shortcircuits().get(payload.getClass());
            
            boolean finished;
            if (shortcircuit != null) {
                Context.ShortcircuitAction action = shortcircuit.perform(ctx);
                
                switch (action) {
                    case PASS:
                        // ShortcircuitLogic asked us to ignore running the actor
                        finished = false;
                        break;
                    case PROCESS:
                        // ShortcircuitLogic asked us to run the actor as we normally would
                        finished = !actor.runner().execute();
                        break;
                    case TERMINATE:
                        // ShortcircuitLogic asked us to terminate the actor
                        finished = true;
                        break;
                    default:
                        // This should never happen
                        throw new IllegalStateException("Unknown action encountered: " + action);
                }
            } else {
                // No shortcircuit for this msg type -- run the actor as normal
                finished = !actor.runner().execute();
            }
            
            // Reset context fields
            ctx.in(null);
            ctx.source(null);
            ctx.destination(null);
            ctx.time(null);
            
            if (!finished) {
                // The actor (regardless of if it's the root actor or a child actor) is still running, so return false to prevent the root
                // actor from being discarded
                return false;
            }
            
            if (actor.parent() == null) {
                // Main/root actor finished, return true to indicate that the main/root actor should be discarded
                return true;
            } else {
                // Child actor finished, remove the child from the parent but return false because the main/root actor isn't effected (it
                // should keep running)
                String childId = dst.getElement(dst.size() - 1);
                actor.parent().children().remove(childId);
                return false;
            }
        } catch (Exception e) {
            // An unhandled exception occured -- return true to indicate that the main/root actor should be discarded
            LOG.error("Actor " + dst + " threw an exception, shutting down the main actor", e);
            return true;
        }
    }
}
