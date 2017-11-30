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
package com.offbynull.actors.stores.redis.client;

import org.apache.commons.lang3.Validate;

/**
 * Actor accessor -- sets and accesses properties of a stored actor.
 * <p>
 * Unless there's a critical error, implementations are required to retry indefinity the operation until it succeeds. The following are not
 * considered to be critical errors...
 * <ul>
 * <li>Connection problems.</li>
 * <li>Redis MULTI/EXEC transactions that fail because a WATCH failed are not critical errors.</li>
 * </ul>
 * @author Kasra Faghihi
 */
public interface ActorAccessor {

    /**
     * Remove checkpoint to process. Must increment the stored checkpoint instance if the checkpoint is hit.
     * @param currentTime current time (epoch millis)
     * @return checkpoint message to process for actor (or {@code null} if the checkpoint wasn't hit)
     * @throws ClientException if there was a problem with redis or the connection to redis
     * @throws IllegalArgumentException if {@code currentTime < 0}
     * @throws IllegalStateException if closed
     */
    Work checkpointMessage(long currentTime) throws ClientException;

    /**
     * Remove next message to process.
     * @return next message to process for actor (or {@code null} if there isn't one)
     * @throws ClientException if there was a problem with redis or the connection to redis
     * @throws IllegalStateException if closed
     */
    Work nextMessage() throws ClientException;

    /**
     * Check to see if this actor is idle.
     * @return {@code true} if this actor is idle, {@code false}
     * @throws ClientException if there was a problem with redis or the connection to redis
     * @throws IllegalStateException if closed
     */
    boolean isIdleAndHasMessages() throws ClientException;

    /**
     * Add new message to process.
     * @param data serialized message data
     * @throws ClientException if there was a problem with redis or the connection to redis
     * @throws IllegalStateException if closed
     */
    void putMessage(byte[] data) throws ClientException;

    /**
     * Delete actor.
     * @throws ClientException if there was a problem with redis or the connection to redis
     * @throws IllegalStateException if closed
     */
    void remove() throws ClientException;

    /**
     * Update the actor. The update is ignored (returns {@code false}) if {@code checkpointInstance} is less than the currently stored
     * checkpoint instance.
     * @param data serilaized actor
     * @param checkpointPayload checkpoint message payload (may be {@code null})
     * @param checkpointTime checkpoint time (must be {@code -1L} if {@code checkpointPayload == null})
     * @param checkpointInstance checkpoint instance of the actor being put in
     * @return {@code true} if the update took place, {@code false} if it didn't
     * @throws NullPointerException if {@code data} is {@code null}
     * @throws IllegalArgumentException if {@code checkpoingPayload} is set, {@code checkpointTime} must be {@code > 0} and vice versa
     * @throws ClientException if there was a problem with redis or the connection to redis
     * @throws IllegalStateException if closed
     */
    boolean update(byte[] data, byte[] checkpointPayload, long checkpointTime, int checkpointInstance) throws ClientException;
    
    /**
     * Piece of work (actor and message) that needs to be processed.
     */
    final class Work {
        private final byte[] actorData;
        private final byte[] messageData;
        private final int checkpointInstance;

        /**
         * Constructs a {@link Work} object.
         * @param actorData serialized actor data
         * @param messageData serialized message data
         * @param checkpointInstance checkpoint instance to set on the actor once its been deserialized
         * @throws NullPointerException if any argument is {@code null}
         */
        public Work(byte[] actorData, byte[] messageData, int checkpointInstance) {
            Validate.notNull(actorData);
            Validate.notNull(messageData);

            this.actorData = actorData.clone();
            this.messageData = messageData.clone();
            this.checkpointInstance = checkpointInstance;
        }

        /**
         * Get serialized actor data.
         * @return serialized actor data
         */
        public byte[] getActorData() {
            return actorData.clone();
        }

        /**
         * Get serialized message data.
         * @return serialized message data
         */
        public byte[] getMessageData() {
            return messageData.clone();
        }

        /**
         * Get checkpoint to set on actor once its been deserialized.
         * @return checkpoint to set on actor once its been deserialized
         */
        public int getCheckpointInstance() {
            return checkpointInstance;
        }
    }
}
