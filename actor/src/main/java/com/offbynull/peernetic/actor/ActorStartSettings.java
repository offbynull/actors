/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.actor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * Actor settings.
 * @author Kasra Faghihi
 */
public final class ActorStartSettings {
    private long hitTime;
    private ActorQueueNotifier notifier;
    private List<Object> messagesToSelf;

    /**
     * Constructs a {@link ActorStartSettings} object. Equivalent to calling {@code new ActorStartSettings(null)}.
     */
    public ActorStartSettings() {
        this(null);
    }

    /**
     * Constructs a {@link ActorStartSettings} object. Equivalent to calling {@code new ActorStartSettings(hitTime, null)}.
     * @param hitTime maximum amount of time to wait before invoking {@link Actor#onStep(long, com.offbynull.peernetic.actor.PullQueue,
     * com.offbynull.peernetic.actor.PushQueue, com.offbynull.peernetic.actor.Endpoint).
     */
    public ActorStartSettings(long hitTime) {
        this(hitTime, null);
    }

    /**
     * Constructs a {@link ActorStartSettings} object. Equivalent to calling {@code new ActorStartSettings(Long.MAX_VALUE, null)}.
     * @param notifier notifier to use for internal queue (can be {@code null)
     */
    public ActorStartSettings(ActorQueueNotifier notifier) {
        this(Long.MAX_VALUE, notifier);
    }

    /**
     * Constructs a {@link ActorStartSettings} object.
     * @param hitTime maximum amount of time to wait before invoking {@link Actor#onStep(long, com.offbynull.peernetic.actor.PullQueue,
     * com.offbynull.peernetic.actor.PushQueue, com.offbynull.peernetic.actor.Endpoint).
     * @param notifier notifier to use for internal queue (can be {@code null)
     * @param messagesToSelf messages to pass to self
     * @throws NullPointerException if {@code messagesToSelf} is {@code null} or contains {@code null}
     */
    public ActorStartSettings(long hitTime, ActorQueueNotifier notifier, Object ... messagesToSelf) {
        Validate.noNullElements(messagesToSelf);
        
        this.hitTime = hitTime;
        this.notifier = notifier;
        this.messagesToSelf = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(messagesToSelf)));
    }

    long getHitTime() {
        return hitTime;
    }

    ActorQueueNotifier getNotifier() {
        return notifier;
    }

    List<Object> getMessagesToSelf() {
        return messagesToSelf;
    }
    
}
