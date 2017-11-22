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
package com.offbynull.actors.core.persister;

import com.offbynull.actors.core.gateways.actor.SerializableActor;
import com.offbynull.actors.core.shuttle.Address;
import com.offbynull.actors.core.shuttle.Message;
import org.apache.commons.lang3.Validate;

/**
 * A piece of work that requires processing (an actor with an incoming message for that actor).
 * @author Kasra Faghihi
 */
public final class PersisterWork {
    private final Message message;
    private final SerializableActor actor;

    /**
     * Constructs a {@link PersistedWork} object.
     * @param message message to process
     * @param actor actor to process
     * @throws NullPointerException if any argument is {@code null}
     */
    public PersisterWork(Message message, SerializableActor actor) {
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
