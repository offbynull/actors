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

/**
 * Use to test an {@link Actor}.
 * @author Kasra Faghihi
 */
public final class ActorTester {
//    private IdCounter idCounter;
//    private Actor actor;
//    
//    /**
//     * Construct an {@link ActorTester} object. 
//     * @param actor actor to test -- once passed in, the actor will be unusable
//     * @throws IllegalStateException if actor already started or stopped.
//     * @throws NullPointerException if any argument is {@code null}
//     */
//    public ActorTester(Actor actor) {
//        Validate.notNull(actor);
//        
//        actor.readyForTesting();
//        this.actor = actor;
//        this.idCounter = new IdCounter();
//    }
//
//    /**
//     * Call the wrapped actor's {@link Actor#onStart(long, java.util.Map) } method.
//     * @param timestamp timestamp to pass in
//     * @param pushQueue push queue to pass in
//     * @param initVars initialization objects to pass in
//     * @throws Exception on error
//     */
//    public void testOnStart(long timestamp, PushQueue pushQueue, Map<Object, Object> initVars) throws Exception {
//        actor.testOnStart(timestamp, pushQueue, initVars);
//    }
//
//    /**
//     * Call the wrapped actor's {@link Actor#onStep(long, com.offbynull.peernetic.common.concurrent.actor.PullQueue,
//     * com.offbynull.peernetic.common.concurrent.actor.PushQueue) } method.
//     * @param timestamp timestamp to pass in
//     * @param pullQueue pull queue to pass in
//     * @param pushQueue push queue to pass in
//     * @return outgoing messages
//     * @throws Exception on error
//     */
//    public long testOnStep(long timestamp, PullQueue pullQueue, PushQueue pushQueue) throws Exception {
//        return actor.testOnStep(timestamp, pullQueue, pushQueue);
//    }
//
//    /**
//     * Call the wrapped actor's {@link Actor#onStop(long, com.offbynull.peernetic.common.concurrent.actor.PushQueue) } method.
//     * @param timestamp timestamp to pass in
//     * @param pushQueue push queue to pass in
//     * @throws Exception on error
//     */
//    public void testOnStop(long timestamp, PushQueue pushQueue) throws Exception {
//        actor.testOnStop(timestamp, pushQueue);
//    }
//    
//    /**
//     * Generate a push queue.
//     * @param requestTimeoutManager request timeout manager to pass in
//     * @param requestIdMap request id map to pass in
//     * @param outgoingMap collection of outgoing messages to populate
//     * @return a new push queue
//     */
//    public PushQueue createPushQueue(TimeoutManager<Object> requestTimeoutManager, Map<Object, IncomingRequest> requestIdMap,
//            MultiMap<Endpoint, Outgoing> outgoingMap) {
//        return new PushQueue(idCounter, requestTimeoutManager, requestIdMap, outgoingMap);
//    }
//    
//    /**
//     * Generate a pull queue.
//     * @param responseTimeoutManager response timeout manager to pass in
//     * @param incoming incoming messages to pass in
//     * @return a new pull queue
//     */
//    public PullQueue createPullQueue(TimeoutManager<Object> responseTimeoutManager, Collection<Incoming> incoming) {
//        return new PullQueue(responseTimeoutManager, incoming);
//    }
}
