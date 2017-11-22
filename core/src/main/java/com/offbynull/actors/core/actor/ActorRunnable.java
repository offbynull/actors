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
package com.offbynull.actors.core.actor;

import com.offbynull.actors.core.actor.Context.BatchedCreateActorCommand;
import com.offbynull.actors.core.actor.Context.BatchedOutgoingMessage;
import com.offbynull.actors.core.actor.Context.CheckpointRestoreLogic;
import com.offbynull.actors.core.actor.Context.ShortcircuitLogic;
import static com.offbynull.actors.core.actor.Context.SuspendFlag.FORWARD_AND_RELEASE;
import static com.offbynull.actors.core.actor.Context.SuspendFlag.FORWARD_AND_RETURN;
import static com.offbynull.actors.core.actor.Context.SuspendFlag.RELEASE;
import com.offbynull.actors.core.shuttle.Shuttle;
import com.offbynull.actors.core.shuttle.Message;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;
import com.offbynull.actors.core.shuttle.Address;
import com.offbynull.actors.core.shuttles.simple.Bus;
import com.offbynull.actors.core.shuttles.simple.SimpleShuttle;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.offbynull.actors.core.checkpoint.Checkpointer;

final class ActorRunnable implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ActorRunnable.class);

    private final String prefix;
    private final Bus bus;
    private final SimpleShuttle incomingShuttle;
    private final Runnable failHandler;
    private final ActorRunner owner;
    private final Checkpointer checkpointer;

    ActorRunnable(
            String prefix,
            Bus bus,
            Runnable failHandler,
            ActorRunner owner,
            Checkpointer checkpointer) {
        Validate.notNull(prefix);
        Validate.notNull(bus);
        Validate.notNull(failHandler);
        Validate.notNull(owner);
        Validate.notNull(checkpointer);
        Validate.notEmpty(prefix);

        this.prefix = prefix;
        this.bus = bus;
        this.incomingShuttle = new SimpleShuttle(prefix, bus);
        this.failHandler = failHandler;
        this.owner = owner;
        this.checkpointer = checkpointer;
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
                failHandler.run();
            } catch (RuntimeException innerRe) {
                LOG.error("Handler failed", innerRe);
            } 
        } catch (RuntimeException re) {
            LOG.error("Internal error encountered", re);
            
            // Invoke critical failure handler
            try {
                failHandler.run();
            } catch (RuntimeException innerRe) {
                LOG.error("Handler failed", innerRe);
            } 
        } finally {
            bus.close();
        }
    }

    private void processManagementMessage(Object msg, Map<String, LoadedActor> actors, List<Message> outgoingMessages,
            Map<String, Shuttle> outgoingShuttles) {
        LOG.debug("Processing management message: {}", msg);
        if (msg instanceof AddActor) {
            AddActor aam = (AddActor) msg;
            
            Address self = Address.of(prefix, aam.getId());
            CoroutineRunner actorRunner = new CoroutineRunner(aam.getActor());
            Context ctx = new Context(actorRunner, self);
            
            actorRunner.setContext(ctx);
            
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
        
        Address actorAddr = Address.of(dstPrefix, dstActorId);

        LoadedActor loadedActor = actors.get(dstActorId);
        Context ctx;
        if (loadedActor == null) {
            LOG.warn("Actor not found in memory for {} (dst={} msg={})", actorAddr, dst, msg);
            ctx = checkpointer.restore(actorAddr);
            
            if (ctx == null) {
                LOG.warn("Actor not found in checkpoint for {}", actorAddr);
                return;
            } else {
                LOG.debug("Actor found in checkpoint: id={}", actorAddr);
                actors.put(dstActorId, new LoadedActor(ctx));
                
                // Get restore logic to perform
                CheckpointRestoreLogic restoreLogic = ctx.checkpoint();
                
                // Reset restored context state
                ctx.copyAndClearOutgoingMessages();
                ctx.checkpoint(null);
                ctx.mode(RELEASE);
                
                // Perform restore logic
                restoreLogic.perform(ctx);
            }
        } else {
            ctx = loadedActor.context;
        }
        
        boolean shutdown = fire(ctx, src, dst, Instant.now(), msg);
        if (shutdown) {
            LOG.debug("Actor shut down {} -- removing from memory and removing from checkpoint", actorAddr);
            checkpointer.delete(actorAddr);
            actors.remove(dstActorId);
        } else {
            if (ctx.checkpoint() != null) {
                LOG.debug("Actor requests checkpoint {} -- removing from memory and adding to checkpoint", actorAddr);
                checkpointer.save(ctx);
                actors.remove(dstActorId);
            }
        }

        // Queue up new actors
        List<BatchedCreateActorCommand> batchedCreateActorCommands = ctx.copyAndClearNewRoots();
        for (BatchedCreateActorCommand batchedCreateActorCommand : batchedCreateActorCommands) {
            owner.addActor(
                    batchedCreateActorCommand.getId(),
                    batchedCreateActorCommand.getActor(),
                    batchedCreateActorCommand.getPrimingMessages());
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

    void addActor(String id, Coroutine coroutine, Object... primingMessages) {
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
        private final Context context;

        LoadedActor(Context context) {
            Validate.notNull(context);
            this.context = context;
        }
    }

    
    
    
    
    
    
    
    
    /**
     * Fire a message to the actor associated with this context. If the destination is a child actor, the message will be correctly routed.
     * @param ctx starting context
     * @param src source
     * @param dst destination
     * @param time execution time
     * @param msg incoming message
     * @return {@code false} if the actor is still active, {@code true} if it should be discarded
     * @throws NullPointerException if any argument is {@code null}
     */
    public static boolean fire(Context ctx, Address src, Address dst, Instant time, Object msg) {
        Validate.notNull(ctx);
        Validate.notNull(src);
        Validate.notNull(dst);
        Validate.notNull(time);
        Validate.notNull(msg);


        // Walk up the context chain until you reach the topmost actor
        while (ctx.parent() != null) {
            ctx = ctx.parent();
        }
        
        return fireRecurse(ctx, src, dst, time, msg);
    }
    
    private static boolean fireRecurse(Context ctx, Address src, Address dst, Instant time, Object msg) {
        if (ctx == null) {
            return false;
        }
        
        
        
        if (ctx.self().equals(dst)) {
            // The message is for us. There's no further child to recurse to so process and get out.
            ctx.mode(RELEASE);
            boolean done = invoke(ctx, src, dst, time, msg);
            return done;
        }
        
        
        
        
        if (ctx.intercept()) {
            // The message is for one of our children, but we want to intercept it....
            boolean done = invoke(ctx, src, dst, time, msg);
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
        Context childCtx = ctx.getChildContext(childId);
        if (childCtx != null) {
            fireRecurse(childCtx, src, dst, time, msg);
        }

        
        
        
        if (ctx.intercept() && ctx.mode() == FORWARD_AND_RETURN) {
            // If we intercepted this message and forwarded it + asked control to be returned back to the forwarder
            boolean done = invoke(ctx, src, dst, time, msg);
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
    
    private static boolean invoke(Context ctx, Address src, Address dst, Instant time, Object msg) {
        if (ctx.ruleSet().evaluate(src, msg.getClass()) != RuleSet.AccessType.ALLOW) {
            LOG.warn("Actor ruleset rejected message: id={} message={}", dst, msg);
            return false;
        }

        
        LOG.debug("Processing message from {} to {} {}", src, dst, msg);
        
        ctx.in(msg);
        ctx.source(src);
        ctx.destination(dst);
        ctx.time(time);
        
        try {
            ShortcircuitLogic shortcircuit = ctx.shortcircuits().get(msg.getClass());
            
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
                        finished = !ctx.runner().execute();
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
                finished = !ctx.runner().execute();
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
            
            if (ctx.parent() == null) {
                // Main/root actor finished, return true to indicate that the main/root actor should be discarded
                return true;
            } else {
                // Child actor finished, remove the child from the parent but return false because the main/root actor isn't effected (it
                // should keep running)
                String childId = dst.getElement(dst.size() - 1);
                ctx.parent().children().remove(childId);
                return false;
            }
        } catch (Exception e) {
            // An unhandled exception occured -- return true to indicate that the main/root actor should be discarded
            LOG.error("Actor " + dst + " threw an exception, shutting down the main actor", e);
            return true;
        }
    }
}
