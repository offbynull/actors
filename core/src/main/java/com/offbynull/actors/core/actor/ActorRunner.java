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

import com.offbynull.actors.core.checkpoint.NullCheckpointer;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.actors.core.shuttle.Address;
import com.offbynull.actors.core.shuttle.Message;
import com.offbynull.actors.core.shuttle.Shuttle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.offbynull.actors.core.checkpoint.Checkpointer;

/**
 * Container used to execute actors.
 * <p>
 * The following usage example creates an instance of {@link ActorRunner}, adds an actor to it, and then shuts it down.
 * <pre>
 * // Create an ActorRunner. The address of all actors added will be prefixed with "local"
 * ActorRunner actorRunner = new ActorRunner("local");
 * 
 * // Add a new coroutine actor with the address "local:actor2". As soon as the actor is added, it will receive 2 incoming messages from
 * // itself: "start1" and "start2".
 * Coroutine myCoroutineActor = ...;
 * actorRunner.addActor("actor2", myCoroutineActor, "start1", "start2");
 * 
 * 
 * ... do some unrelated stuff here ...
 * 
 * 
 * 
 * // Shutdown the ActorRunner
 * actorRunner.close();
 * </pre>
 * 
 * All actors assigned to a runner can send messages to each other without any additional setup.
 * <p>
 * If you want outside components to be able to send messages to actors assigned to a runner, you'll need to pass those
 * outside components a reference to the {@link Shuttle} returned by {@link #getIncomingShuttle() }.
 * <p>
 * Similarly, if actors assigned to a runner are going to send messages to outside components, the {@link Shuttle}s for
 * those outgoing components need to be added using {@link #addOutgoingShuttle(com.offbynull.actors.core.shuttle.Shuttle) }.
 * <p>
 * If an actor tries to send a message to an address for which no outgoing shuttle has been added, that message is silently discarded.
 * 
 * @author Kasra Faghihi
 */
public final class ActorRunner implements AutoCloseable {
    
    private static final Logger LOG = LoggerFactory.getLogger(ActorRunner.class);
    
    private final String prefix;
    private final ActorThread[] threads;
    private final RunnerShuttle shuttle;

    /**
     * Create an {@link ActorRunner} instance. with the number of threads set to the number of processors available on the system.
     * Equivalent to calling {@code ActorRunner.create(prefix, Runtime.getRuntime().availableProcessors())}.
     * @param prefix address prefix to use for actors that get added to this runner
     * @throws NullPointerException if any argument is {@code null}
     * @return new actor runner
     */
    public static ActorRunner create(String prefix) {
        return ActorRunner.create(prefix, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Create an {@link ActorRunner} instance. Equivalent to calling
     * {@code ActorRunner.create(prefix, threadCount, new NullCheckpointer())}.
     * @param prefix address prefix to use for actors that get added to this runner
     * @param threadCount number of threads to use for this runner
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code threadCount < 1}
     * @return new actor runner
     */
    public static ActorRunner create(String prefix, int threadCount) {
        return ActorRunner.create(prefix, threadCount, new NullCheckpointer());
    }

    /**
     * Create an {@link ActorRunner} instance.
     * @param prefix address prefix to use for actors that get added to this runner
     * @param threadCount number of threads to use for this runner
     * @param checkpointer checkpointer
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code threadCount < 1}
     * @return new actor runner
     */
    public static ActorRunner create(String prefix, int threadCount, Checkpointer checkpointer) {
        Validate.notNull(prefix);
        Validate.notNull(checkpointer);
        Validate.isTrue(threadCount > 0);

        ActorRunner ret = new ActorRunner(prefix, threadCount);
        
        // Handler to call if any of the threads encounter a problem while they're running. If any thread encounters a critical error, then
        // all threads must be shut down!
        Runnable criticalFailureHandler = () -> {
            LOG.error("Critical failure handler invoked! Signalling all threads to close.");
            
            for (ActorThread thread : ret.threads) {
                // Wrap in try catch just to be safe... we want to make sure close is called on every thread.
                try {
                    thread.close();
                } catch (RuntimeException e) {
                    LOG.error("Error signalling thread to close", e);
                }
            }
        };
        
        // Start threads
        try {
            for (int i = 0; i < threadCount; i++) {
                ret.threads[i] = ActorThread.create(prefix, ret.shuttle, criticalFailureHandler, ret, checkpointer);
            }
        } catch (RuntimeException e) {
            // A problem happened while creating new threads... shut down any threads that were created.
            for (ActorThread thread : ret.threads) {
                if (thread != null) {
                    thread.close(); // Signal shutdown, but don't wait until thread actually stops before returning
                }
            }
            
            throw e;
        }
        
        return ret;
    }
    
    private ActorRunner(String prefix, int threadCount) {
        Validate.notNull(prefix);
        Validate.isTrue(threadCount > 0);
        
        this.prefix = prefix;
        this.threads = new ActorThread[threadCount];
        this.shuttle = new RunnerShuttle();
    }
    
    /**
     * Shuts down this runner. Blocks until the internal threads that execute actors terminate before returning.
     * @throws InterruptedException if interrupted while waiting for shutdown
     */
    @Override
    public void close() throws InterruptedException {
        // Signal threads to close
        for (ActorThread thread : threads) {
            thread.close();
        }
        
        // Wait until threads are closed, throws interrupted exception
        for (ActorThread thread : threads) {
            thread.join();
        }
    }

    /**
     * Blocks until the internal threads that execute actors terminate.
     * @throws InterruptedException if interrupted while waiting
     */
    public void join() throws InterruptedException {
        for (ActorThread thread : threads) {
            thread.join();
        }
    }
    
    /**
     * Get the shuttle used to receive messages.
     * @return shuttle for incoming messages to this runner
     */
    public Shuttle getIncomingShuttle() {
        return shuttle;
    }

    /**
     * Queue an actor to be added. Note that this method queues an actor to be added rather than adding it right away. As such, this
     * method will likely return before the actor in question is added, and any error during encountered during adding will not be
     * known to the caller. On error (e.g. actor with same id already exists), this runner terminates.
     * <p>
     * If this runner has been shutdown prior to calling this method, this method does nothing.
     * @param id id to use for actor. For example, if the prefix for this runner is "runner", and the id of the actor being add is "test",
     * that actor will be accessible via the address "runner:test".
     * @param actor actor being added
     * @param primingMessages messages to send to {@code actor} (shown as coming from itself) once its been added
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    public void addActor(String id, Coroutine actor, Object... primingMessages) {
        mapIdToActorThread(id).addActor(id, actor, primingMessages);
    }

    /**
     * Queue an actor to be remove. Note that this method queues an actor to be removed rather than removing it right away. As such, this
     * method will likely return before the actor in question is removed, and any error during encountered during removal will not be
     * known to the caller. On error (e.g. actor with id doesn't exist), this runner terminates.
     * <p>
     * If this runner has been shutdown prior to calling this method, this method does nothing.
     * <p>
     * If this runner doesn't contain an actor with the id {@code id}, nothing will be removed.
     * @param id id of actor to remove
     * @throws NullPointerException if any argument is {@code null}
     */
    public void removeActor(String id) {
        mapIdToActorThread(id).removeActor(id);
    }

    /**
     * Queue an outgoing shuttle to be added. When an actor sends a message, that message will be forwarded to the appropriate outgoing
     * shuttle (based on the prefix of the destination address). If no outgoing shuttle is found, the message is silently discarded.
     * <p>
     * Note that this operation queues a shuttle to be added rather than adding it right away. As such, this method will likely
     * return before the add operation completes, and any error encountered during the operation will not be known to the caller. On error
     * (e.g. outgoing shuttle with same prefix already exists), this runner terminates.
     * <p>
     * If this runner has been shutdown prior to calling this method, this method does nothing.
     * <p>
     * @param shuttle outgoing shuttle to add
     * @throws NullPointerException if any argument is {@code null}
     */
    public void addOutgoingShuttle(Shuttle shuttle) {
        for (ActorThread thread : threads) {
            thread.addOutgoingShuttle(shuttle);
        }
    }

    /**
     * Queue an outgoing shuttle for removal.
     * <p>
     * Note that this operation queues a shuttle to be added rather than adding it right away. As such, this method will likely
     * return before the add operation completes and any error encountered during the operation will not be known to the caller.
     * <p>
     * If this runner has been shutdown prior to calling this method, this method does nothing. On error (e.g. outgoing shuttle
     * with prefix doesn't exist), this runner terminates.
     * <p>
     * @param prefix address prefix for shuttle to remove
     * @throws NullPointerException if any argument is {@code null}
     */
    public void removeOutgoingShuttle(String prefix) {
        for (ActorThread thread : threads) {
            thread.removeOutgoingShuttle(prefix);
        }
    }
    
    private ActorThread mapIdToActorThread(String id) {
        int idx = mapIdToIndex(id);
        return threads[idx];
    }

    private int mapIdToIndex(String id) {
        // hash may be negative, so modding hash value may return negative value... so make sure to get absolute value
        return Math.abs(fnv1a32Hash(id) % threads.length);
    }
    
    // http://programmers.stackexchange.com/questions/49550/which-hashing-algorithm-is-best-for-uniqueness-and-speed
    // http://codereview.stackexchange.com/questions/39515/implementation-of-the-fnv-1a-hash-algorithm-for-32-and-64-bit
    // https://en.wikipedia.org/wiki/Fowler%E2%80%93Noll%E2%80%93Vo_hash_function
    private int fnv1a32Hash(String str) {
        final long fnvPrime = 116777619L;
        final long fnvOffsetBasis = 2166136261L;
        
        byte[] data = str.getBytes(Charsets.UTF_8);
        
        long hash = fnvOffsetBasis;
        for (byte b : data) {
            hash ^= (long) (b & 0xFF); // FROM WIKIPEDIA: In the above pseudocode, all variables are unsigned integers. All variables,
                                       // except for byte_of_data, have the same number of bits as the FNV hash. The variable, byte_of_data,
                                       // is an 8 bit unsigned integer.
            hash *= fnvPrime;
        }

        return (int) (hash & 0xFFFFFFFFL);
    }
    
    private final class RunnerShuttle implements Shuttle {

        @Override
        public String getPrefix() {
            return prefix;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void send(Collection<Message> messages) {
            Validate.notNull(messages);
            Validate.noNullElements(messages);


            List<Message>[] threadMessagesList = new List[threads.length];
            for (int i = 0; i < threads.length; i++) {
                threadMessagesList[i] = new ArrayList<>(messages.size());
            }
            
            messages.stream().forEach(x -> {
                try {
                    Address dst = x.getDestinationAddress();
                    String dstPrefix = dst.getElement(0);
                    Validate.isTrue(dstPrefix.equals(prefix));
                    
                    String id = dst.getElement(1);
                    int idx = mapIdToIndex(id);

                    threadMessagesList[idx].add(x);
                } catch (Exception e) {
                    LOG.error("Error mapping message to thread: " + x, e);
                }
            });

            for (int i = 0; i < threads.length; i++) {
                List<Message> threadMessages = threadMessagesList[i];
                if (threadMessages.isEmpty()) {
                    continue;
                }
                
                LOG.debug("Shuttling {} messages to thread {}", threadMessages.size(), i);
                threads[i].getIncomingShuttle().send(threadMessages);
            }
        }
    }
}
