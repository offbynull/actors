/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.coroutines.user.CoroutineRunner;
import com.offbynull.peernetic.core.actor.Actor;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.Address;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * Forwards an incoming message to a {@link Subcoroutine} based on the destination id of that incoming message.
 * <p>
 * For example, imagine this router has the id "myrouter" and is owned by an {@link Actor} with the address "local:myactor". This router has
 * one {@link Subcoroutine} assigned to it with the id "child1". A message arrives with the destination address
 * "local:myactor:myrouter:child1". The actor should forward that message to this router, and this router should forward that message to the
 * {@link Subcoroutine} with the id "child1".
 * @author Kasra Faghihi
 */
public final class SubcoroutineRouter {

    private final Address id;
    private final Context context;
    private final Map<String, CoroutineRunner> idMap;
    private final Controller controller;

    /**
     * Constructs a {@link SubcoroutineRouter}.
     * @param id of this router -- incoming messages destined for this id should trigger this router
     * @param context actor context
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code id} is empty
     */
    public SubcoroutineRouter(Address id, Context context) {
        Validate.notNull(id);
        Validate.notNull(context);
        Validate.isTrue(!id.isEmpty());
        this.id = id;
        this.context = context;
        idMap = new HashMap<>();
        controller = new Controller();
    }

    /**
     * Get the controller for this router. The controller allows you to add {@link Subcoroutine}s to and remove {@link Subcoroutine}s from
     * this router. It is safe to use this class to add/remove {@link Subcoroutine}s even if doing so from within a {@link Subcoroutine}
     * invoked by the router that owns this controller.
     * @return controller for this router
     */
    public Controller getController() {
        return controller;
    }

    /**
     * Route the current incoming message to the appropriate {@link Subcoroutine}. If id of the incoming message doesn't map to any
     * {@link Subcoroutine} assigned to this router, this method returns without throwing an exception.
     * @return {@code true} if the forward was routed to a {@link Subcoroutine}, {@code false} otherwise
     * @throws Exception if the {@link Subcoroutine} forwarded to threw an exception
     */
    public boolean forward() throws Exception {
        Address incomingId = context.getDestination().removePrefix(context.getSelf());
        if (!id.isPrefixOf(incomingId)) {
            return false;
        }
        
        Address subId = incomingId.removePrefix(id);
        if (subId.isEmpty()) {
            return false;
        }
        
        String key = subId.getElement(0);

        CoroutineRunner runner = idMap.get(key);

        boolean forwarded = false;
        if (runner != null) {
            boolean running = runner.execute();
            if (!running) {
                idMap.remove(key);
            }
            forwarded = true;
        }
        
        return forwarded;
    }

    /**
     * Get the id of this router. Incoming messages destined for this id should trigger this router to run. The id returned must be an
     * absolute id, not a relative id. Meaning that if there's a hierarchy, each one should return the full id, not the id relative to its
     * parent.
     * @return id
     */
    public Address getId() {
        return id;
    }
    
    /**
     * Controller to add {@link Subcoroutine}s to and remove {@link Subcoroutine}s from a router. It is safe to use this class to add/remove
     * {@link Subcoroutine}s even if doing so from within a {@link Subcoroutine} invoked by the router that owns this controller.
     */
    public final class Controller {
        /**
         * Add a {@link Subcoroutine} to the router that owns this controller. If you choose to prime the {@link Subcoroutine} that's being
         * added (by choosing either {@link AddBehaviour#ADD_PRIME} or {@link AddBehaviour#ADD_PRIME_NO_FINISH} for {@code addBehaviour}),
         * this method will forward the current incoming message to it.
         * @param subcoroutine subcoroutine to add
         * @param addBehaviour add behaviour
         * @throws NullPointerException if any argument is {@code null}
         * @throws IllegalArgumentException if a subcoroutine with this id already assigned to the owning router, or if the id of
         * {@code subcoroutine} isn't a <b>direct</b> child of the owning router
         * @throws IllegalStateException if {@code addBehaviour} was set to {@link AddBehaviour#ADD_PRIME_NO_FINISH}, but
         * {@code subcoroutine} finished after priming
         * @throws Exception if {@code subcoroutine} threw an exception while priming ({@code addBehaviour} has to be set to either
         * {@link AddBehaviour#ADD_PRIME} or {@link AddBehaviour#ADD_PRIME_NO_FINISH} for this to be possible)
         */
        public void add(Subcoroutine<?> subcoroutine, AddBehaviour addBehaviour) throws Exception {
            Validate.notNull(subcoroutine);
            Validate.notNull(addBehaviour);

            Address subcoroutineId = subcoroutine.getId();
            Address suffix = subcoroutineId.removePrefix(id);
            Validate.isTrue(suffix.size() == 1);
            
            String key = suffix.getElement(0);

            CoroutineRunner newRunner = new CoroutineRunner(x -> subcoroutine.run(x));
            newRunner.setContext(context);
            CoroutineRunner existing = idMap.putIfAbsent(key, newRunner);
            Validate.isTrue(existing == null);

            switch (addBehaviour) {
                case ADD:
                    break;
                case ADD_PRIME:
                    forceForward(key, false);
                    break;
                case ADD_PRIME_NO_FINISH:
                    forceForward(key, true);
                    break;
                default:
                    throw new IllegalStateException(); // should never happen
            }
        }
        
        /**
         * Removes a {@link Subcoroutine} from the router that owns this controller.
         * @param id id of subcoroutine to remove
         * @throws NullPointerException if any argument is {@code null}
         * @throws IllegalArgumentException if a subcoroutine with this id is not assigned to the owning router, or if {@code id} isn't a
         * <b>direct</b> child of the owning router
         */
        public void remove(Address id) {
            Validate.notNull(id);
            
            Address suffix = id.removePrefix(SubcoroutineRouter.this.id);
            Validate.isTrue(suffix.size() == 1);
            
            String key = suffix.getElement(0);
            
            CoroutineRunner old = idMap.remove(key);
            Validate.isTrue(old != null);
        }

        private boolean forceForward(String id, boolean mustNotFinish) throws Exception {
            Validate.notNull(id);

            CoroutineRunner runner = idMap.get(id);

            boolean forwarded = false;
            if (runner != null) {
                boolean running = runner.execute();
                if (!running) {
                    Validate.validState(!mustNotFinish, "Runner pointed to by suffix was not supposed to finish");
                    idMap.remove(id);
                }
                forwarded = true;
            }

            return forwarded;
        }
    }
    
    /**
     * Behaviour when adding a subcoroutine via {@link Controller#add(com.offbynull.peernetic.core.actor.helpers.Subcoroutine,
     * com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour) }.
     */
    public enum AddBehaviour {
        /**
         * Add the {@link Subcoroutine} to {@link SubcoroutineRouter}.
         */
        ADD,
        /**
         * Add the {@link Subcoroutine} to {@link SubcoroutineRouter}, and prime it with the current incoming message.
         */
        ADD_PRIME,
        /**
         * Add the {@link Subcoroutine} to {@link SubcoroutineRouter}, prime it with the current incoming message, and make sure it doesn't
         * finish after priming.
         */
        ADD_PRIME_NO_FINISH;
    }
}
