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
import com.offbynull.coroutines.user.CoroutineReader;
import com.offbynull.coroutines.user.CoroutineRunner;
import com.offbynull.coroutines.user.CoroutineWriter;
import com.offbynull.coroutines.user.SerializedState;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.Validate;

/**
 * Serializable actor.
 * @author Kasra Faghihi
 */
public final class SerializableActor implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final SerializableActor parent;
    private final Map<String, SerializableActor> children; 
    
    private final SerializedState runner;
    private final Context context;

    
    /**
     * Get the address for this actor.
     * @return address
     */
    public Address getSelf() {
        return context.self();
    }

    /**
     * Get checkpoint message payload for this actor.
     * @return checkpoint message
     */
    public Object getCheckpointPayload() {
        return context.checkpointPayload();
    }

    /**
     * Get checkpoint time for this actor.
     * @return checkpoint time
     */
    public long getCheckpointTimeout() {
        return context.checkpointTimeout();
    }

    // The checkpoint instance is a unique identifier that's used for the race condition where an actor is processing a message but it
    // takes so long that the checkpoint hits. If the checkpoint hits, the unique identifier updates -- any previously running instance of
    // the actor will have a different checkpoint instance and won't be let back into storage (it'll be silently discarded).
     
    /**
     * Get checkpoint instance for this actor.
     * @return checkpoint instance
     */
    public int getCheckpointInstance() {
        return context.checkpointInstance();
    }
     
    /**
     * Set checkpoint instance for this actor.
     * @param instance new instance
     */
    public void setCheckpointInstance(int instance) {
        context.checkpointInstance(instance);
    }

    /**
     * Get checkpoint updated flag for this actor. Indicates that the user wants the current state of the actor to be a checkpoint.
     * @return checkpoint updated flag
     */
    public boolean getCheckpointUpdated() {
        return context.checkpointUpdated();
    }

    /**
     * set checkpoint updated flag for this actor. Indicates that the user wants the current state of the actor to be a checkpoint.
     * @param checkpointUpdated checkpoint updated flag
     */
    public void setCheckpointUpdated(boolean checkpointUpdated) {
        context.checkpointUpdated(checkpointUpdated);
    }
    
    
    static SerializableActor serialize(Actor actor) {
        Validate.notNull(actor);
        Validate.isTrue(actor.parent() == null);
        
        CoroutineWriter coroutineWriter = new CoroutineWriter();
        
        return serialize(null, actor, coroutineWriter);
    }

    private static SerializableActor serialize(SerializableActor parentActor, Actor actorToConvert, CoroutineWriter coroutineWriter) {
        Map<String, SerializableActor> children = new HashMap<>();
        SerializedState runner = coroutineWriter.deconstruct(actorToConvert.runner());
        Context context = actorToConvert.context();

        SerializableActor actor = new SerializableActor(parentActor, children, runner, context);
        
        for (Entry<String, Actor> childActorToConvertEntry : actorToConvert.children().entrySet()) {
            String childActorId = childActorToConvertEntry.getKey();
            Actor childActorToConvert = childActorToConvertEntry.getValue();
            
            SerializableActor childActor = serialize(actor, childActorToConvert, coroutineWriter);
            
            children.put(childActorId, childActor);
        }
        
        return actor;
    }








    static Actor deserialize(SerializableActor actor) {
        Validate.notNull(actor);
        Validate.isTrue(actor.parent == null);
        
        CoroutineReader coroutineReader = new CoroutineReader();
        
        return deserialize(null, actor, coroutineReader);
    }

    private static Actor deserialize(Actor parentActor, SerializableActor actorToConvert, CoroutineReader coroutineReader) {
        Map<String, Actor> children = new HashMap<>();
        CoroutineRunner runner = coroutineReader.reconstruct(actorToConvert.runner);
        Context context = actorToConvert.context;

        Actor actor = new Actor(parentActor, runner, children, context);
        
        for (Entry<String, SerializableActor> childActorToConvertEntry : actorToConvert.children.entrySet()) {
            String childActorId = childActorToConvertEntry.getKey();
            SerializableActor childActorToConvert = childActorToConvertEntry.getValue();
            
            Actor childActor = deserialize(actor, childActorToConvert, coroutineReader);
            
            children.put(childActorId, childActor);
        }
        
        return actor;
    }






    
    
    

    private SerializableActor(SerializableActor parent, Map<String, SerializableActor> children, SerializedState runner, Context context) {
        this.parent = parent;
        this.children = children;
        this.runner = runner;
        this.context = context;
    }
}
