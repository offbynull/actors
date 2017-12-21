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

import com.offbynull.actors.address.Address;
import com.offbynull.actors.shuttle.Message;
import java.io.Closeable;
import java.util.Arrays;
import java.util.Collection;
import org.apache.commons.lang3.Validate;

/**
 * An actor storage engine keeps track of actors, their incoming message queues, and their checkpoints. It's the mechanism by which actors
 * and their executions are distributed across multiple servers.
 * <p>
 * Each server has their actor storage engine pointing to the same backend (e.g. an RDBMS sever), and as such can...
 * <ul>
 * <li>store a message into an actor's message queue.</li> 
 * <li>store an actor.</li>
 * <li>replace an actor.</li>
 * <li>remove an actor.</li>
 * <li>pull the next piece of work to execute.</li>
 * </ul>
 * <p>
 * The piece of work being pulled for execution can come from 2 places: an actor's message queue or an actor's checkpoint. Healthy actors
 * will get pulled down along with the next message in their message queue. Unhealthy actors will get pulled down from their checkpointed
 * state along with their checkpoint message.
 * <p>
 * How does a healthy actor differ from an unhealthy actor? An actor that hasn't re-set its checkpoint by its self-imposed checkpoint
 * timeout is considered unhealthy. This is the case even if that actor is currently in the middle of executing a message. The idea
 * behind this is that an actor ...
 * <ul>
 * <li>must receive the interactions it expects by the checkpoint timeout.</li>
 * <li>must release control by the checkpoint timeout (technically must not block at all).</li>
 * </ul>
 * <p>
 * For example, if an actor inadvertently enters into a tight loop and doesn't return before the checkpoint timeout, it'll be considered as
 * unhealthy and the storage engine will revert it to its checkpoint state. The existing instance of the actor may still be spinning in the
 * tight loop when this happens, but it's essentially considered as dead. If it ever ends up exiting the tight loop and returning control,
 * it won't be allowed back in to storage.
 * <p>
 * Another example is if an actor is expecting to receive a response to a message it sent out. If it doesn't receive the message before the
 * checkpoint timeout, it'll be considered as unhealthy and the store engine will revert it to checkpoint state. The checkpoint could have
 * been triggered because the the server responsible for responding to that message went down, or maybe it was under heavy load and hadn't
 * gotten a chance to respond in time. Either way, the actor will revert and perform whatever logic it deems appropriate (e.g. notifying
 * users that its checkpoint was hit, retrying immediately, waiting and retrying, etc..).
 * <p>
 * <strong>Basic Usage</strong>
 * <p>
 * To...
 * <ul>
 * <li>pull an actor along with a message for that actor to execute: {@link #take()}</li>
 * <li>push an actor that's finished executing a message back in: {@link #store(com.offbynull.actors.gateways.actor.SerializableActor)}</li>
 * <li>push a new actor: {@link #store(com.offbynull.actors.gateways.actor.SerializableActor)}</li>
 * <li>push a new messages for actors: {@link #store(com.offbynull.actors.shuttle.Message...)}</li>
 * <li>remove an actor: {@link #discard(com.offbynull.actors.address.Address)}</li>
 * </ul>
 * <p>
 * <strong>Other Details</strong>
 * <p>
 * Implementations must be robust. It should only throw exceptions for critical errors. For example, if the implementation encounters
 * connectivity issues, rather than throwing an exception it should block and retry until the issue has been resolved.
 * @author Kasra Faghihi
 */
public interface Store extends Closeable {

    /**
     * Puts actor into storage.
     * <p>
     * If the actor being stored already exists, it must have a checkpoint instance equal to the one in storage. The idea is that
     * if the actor is getting pushed back in after having finished executing a message, it will have the same checkpoint instance.
     * <p>
     * The exception is when a checkpoint hits. When {@link #take()} determines that an actor's checkpoint timeout has elapsed, it
     * increments the checkpoint instance both in storage and in the actor that it returns. As such, if the actor was executing prior to the
     * checkpoint being hit, it won't be let back in once it finishes because its checkpoint instance will be different.
     * @param actor actor to store
     * @return {@code false} if couldn't be stored because {@code actor} it had an old checkpoint instance
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if the operation could not complete successfully, or if this storage engine has been closed
     */
    boolean store(SerializableActor actor);

    /**
     * Equivalent to calling {@code store(Arrays.asList(messages))}.
     * @param messages messages coming from {@code actor}
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if any of the actor has an invalid address (bad prefix or unexpected size)
     * @throws IllegalStateException if the operation could not complete successfully, or if this storage engine has been closed
     */
    default void store(Message... messages) {
        store(Arrays.asList(messages));
    }
    
    /**
     * Puts incoming messages into storage. If the actor for an incoming message isn't in storage, the message will be silently discarded.
     * @param messages incoming messages
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if any of the actor has an invalid address (bad prefix or unexpected size)
     * @throws IllegalStateException if the operation could not complete successfully, or if this storage engine has been closed
     */
    void store(Collection<Message> messages);
    
    /**
     * Equivalent to calling {@code discard(Address.fromString(address))}.
     * @param address address of actor to discard
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalStateException if the operation could not complete successfully, or if this storage engine has been closed
     */
    default void discard(String address) {
        discard(Address.fromString(address));
    }
    
    /**
     * Removes/discards an actor from storage. Does nothing if the actor being removed doesn't exist.
     * @param address address of actor to discard
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalStateException if the operation could not complete successfully, or if this storage engine has been closed
     */
    void discard(Address address);

    /**
     * Pull a piece of work (message along with the actor responsible for executing it) from storage. If no work is available, this
     * method blocks until work becomes available.
     * <p>
     * The returned work will either be ...
     * <ul>
     * <li><b>an actor in its latest state with the next pending message in its incoming message queue.</b></li>
     * <li><b>an actor in its checkpointed state with its checkpoint message.</b> This only happens if the storage engine determined that
     * the actor's checkpoint timeout elapsed. Both the checkpoint instance of the actor being returned and the checkpoint instance in
     * storage will be incremented, and the checkpoint timeout will be set to infinity so that the checkpoint doesn't get triggered again
     * (it's up to the actor to reset it).</li>
     * </ul>
     * The actor returned will not be eligible for work until it either finishes executing or its checkpoint timeout elapses.
     * @return actor and message for actor
     * @throws IllegalStateException if the operation could not complete successfully, or if this storage engine has been closed
     */
    StoredWork take();
    
    
    
    
    
    
    
    
    
    /**
     * A piece of work that requires processing (an actor with an incoming message for that actor).
     */
    final class StoredWork {
        private final Message message;
        private final SerializableActor actor;

        /**
         * Constructs a {@link StoredWork} object.
         * @param message message to process
         * @param actor actor to process
         * @throws NullPointerException if any argument is {@code null}
         */
        public StoredWork(Message message, SerializableActor actor) {
            Validate.notNull(message);
            Validate.notNull(actor);

            Address actorAddr = actor.getSelf();
            Address dstAddr = message.getDestinationAddress();
            Validate.isTrue(actorAddr.isPrefixOf(dstAddr));

            this.message = message;
            this.actor = actor;
        }

        /**
         * Get message to process.
         * @return message
         */
        public Message getMessage() {
            return message;
        }

        /**
         * Get actor to process.
         * @return actor
         */
        public SerializableActor getActor() {
            return actor;
        }

    }
}
