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
package com.offbynull.actors;

import static com.offbynull.actors.gateway.CommonAddresses.DEFAULT_ACTOR;
import static com.offbynull.actors.gateway.CommonAddresses.DEFAULT_DIRECT;
import static com.offbynull.actors.gateway.CommonAddresses.DEFAULT_LOG;
import static com.offbynull.actors.gateway.CommonAddresses.DEFAULT_SERVLET;
import static com.offbynull.actors.gateway.CommonAddresses.DEFAULT_TIMER;
import com.offbynull.actors.gateways.actor.ActorGateway;
import com.offbynull.actors.gateway.Gateway;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import com.offbynull.actors.gateways.direct.DirectGateway;
import com.offbynull.actors.gateways.log.LogGateway;
import com.offbynull.actors.gateways.timer.TimerGateway;
import com.offbynull.actors.shuttle.Shuttle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import org.apache.commons.lang3.Validate;
import com.offbynull.actors.gateways.actor.stores.memory.MemoryStore;
import java.io.Closeable;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import com.offbynull.actors.gateways.actor.Store;
import com.offbynull.actors.gateways.servlet.ServletGateway;

/**
 * Actor system.
 * <p>
 * Use {@link #builder() } to construct and start an actor system.
 * @author Kasra Faghihi
 */
public final class ActorSystem implements Closeable {
    private final Map<String, Gateway> gateways;

    private ActorSystem(List<Supplier<Gateway>> gatewayFactories) {
        gateways = new HashMap<>();
        
        try {
            for (Supplier<Gateway> gatewayFactory : gatewayFactories) {
                Gateway gateway = gatewayFactory.get();
                String name = gateway.getIncomingShuttle().getPrefix();

                Gateway existing = gateways.putIfAbsent(name, gateway);
                if (existing != null) {
                    try {
                        gateway.close();
                    } catch (IOException e) {
                        // do nothing
                    }
                    
                    throw new IllegalArgumentException("More than 1 gateway has the same prefix: " + name);
                }
            }

            for (Gateway gateway : gateways.values()) {
                bindGatewayToOthers(gateway, gateways.values());
            }
        } catch (RuntimeException re) {
            gateways.values().forEach(g -> IOUtils.closeQuietly(g));
            throw re;
        }
    }
    
    /**
     * Get a gateway by its prefix.
     * @param prefix prefix
     * @param <T> gateway type
     * @return gateway with the prefix {@code prefix}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if no gateway with the prefix {@code prefix} is found
     */
    public <T extends Gateway> T getGateway(String prefix) {
        Validate.notNull(prefix);
        Gateway gateway = gateways.get(prefix);
        
        Validate.isTrue(gateway != null, "%s not found", prefix);
        
        return (T) gateway;
    }

    /**
     * Get the {@link ActorGateway} associated with this actor system (if present). Equivalent to calling {@code getGateway(DEFAULT_ACTOR)}.
     * @return actor gateway
     * @throws IllegalArgumentException if no gateway with the prefix {@code DEFAULT_ACTOR} is found
     * @throws ClassCastException if type of gateway is something other than {@link ActorGateway} -- this shouldn't be the case if
     * {@link Builder#withActorGateway() } or one of its overloads were used
     */
    public ActorGateway getActorGateway() {
        return getGateway(DEFAULT_ACTOR);
    }

    /**
     * Get the {@link DirectGateway} associated with this actor system (if present). Equivalent to calling
     * {@code getGateway(DEFAULT_DIRECT)}.
     * @return direct gateway
     * @throws IllegalArgumentException if no gateway with the prefix {@code DEFAULT_DIRECT} is found
     * @throws ClassCastException if type of gateway is something other than {@link ActorGateway} -- this shouldn't be the case if
     * {@link Builder#withDirectGateway() } or one of its overloads were used
     */
    public DirectGateway getDirectGateway() {
        return getGateway(DEFAULT_DIRECT);
    }

    /**
     * Get the {@link LogGateway} associated with this actor system (if present). Equivalent to calling {@code getGateway(DEFAULT_LOG)}.
     * @return log gateway
     * @throws IllegalArgumentException if no gateway with the prefix {@code DEFAULT_LOG} is found
     * @throws ClassCastException if type of gateway is something other than {@link LogGateway} -- this shouldn't be the case if
     * {@link Builder#withLogGateway() } or one of its overloads were used
     */
    public LogGateway getLogGateway() {
        return getGateway(DEFAULT_LOG);
    }

    /**
     * Get the {@link TimerGateway} associated with this actor system (if present). Equivalent to calling {@code getGateway(DEFAULT_TIMER)}.
     * @return timer gateway
     * @throws IllegalArgumentException if no gateway with the prefix {@code DEFAULT_TIMER} is found
     * @throws ClassCastException if type of gateway is something other than {@link TimerGateway} -- this shouldn't be the case if
     * {@link Builder#withTimerGateway() } or one of its overloads were used
     */
    public TimerGateway getTimerGateway() {
        return getGateway(DEFAULT_TIMER);
    }

    /**
     * Get the {@link ServletGateway} associated with this actor system (if present). Equivalent to calling
     * {@code getGateway(DEFAULT_SERVLET)}.
     * @return servlet gateway
     * @throws IllegalArgumentException if no gateway with the prefix {@code DEFAULT_SERVLET} is found
     * @throws ClassCastException if type of gateway is something other than {@link ServletGateway} -- this shouldn't be the case if
     * {@link Builder#withServletGateway() } or one of its overloads were used
     */
    public ServletGateway getServletGateway() {
        return getGateway(DEFAULT_SERVLET);
    }

    private static void bindGatewayToOthers(Gateway gateway, Collection<Gateway> allGateways) {
        // Add gateway to other gateways
        Shuttle gatewayShuttle = gateway.getIncomingShuttle();
        for (Gateway otherGateway : allGateways) {
//            if (gateway == otherGateway) {
//                continue;
//            }

            otherGateway.addOutgoingShuttle(gatewayShuttle);
        }
    }
    
    @Override
    public void close() {
        gateways.values().forEach(g -> IOUtils.closeQuietly(g));
    }

    /**
     * Waits for the gateways in this system to die.
     * @throws InterruptedException if thread is interrupted while waiting
     */
    public void join() throws InterruptedException {
        for (Gateway gateway : gateways.values()) {
            gateway.join();
        }
    }
    
    /**
     * Create a {@link ActorSystem} builder.
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Create a {@link ActorSystem} builder pre-set with common gateways -- equivalent to calling
     * {@code new Builder().withActorGateway().withDirectGateway().withLogGateway().withTimerGateway().withServletGateway()}.
     * @return new builder
     */
    public static Builder defaultBuilder() {
        return new Builder().withActorGateway().withDirectGateway().withLogGateway().withTimerGateway().withServletGateway();
    }
    
    /**
     * Create a {@link ActorSystem} set with common gateways -- equivalent to calling {@code defaultBuilder().build()}.
     * @return new actor system with common gateways
     */
    public static ActorSystem createDefault() {
        return defaultBuilder().build();
    }
    
    /**
     * Actor system builder.
     */
    public static final class Builder {
        
        private final List<Supplier<Gateway>> gatewayFactories;

        private Builder() {
            gatewayFactories = new ArrayList<>();
        }
        
        /**
         * Add gateway factory to new actor system being built. Using a factory is the preferred way of adding a gateway. The reason is that
         * the gateway being added will be created as needed. It will be isolated to the actor system being built. As such, you won't have
         * to worry about manually closing the gateway if a critical error is encountered.
         * <p>
         * Usage example... {@code withGatewayFactory(() -> SshGateway.create("ssh", 20)) }
         * @param gatewayFactory gateway factory
         * @return this builder
         */
        public Builder withGatewayFactory(Supplier<Gateway> gatewayFactory) {
            gatewayFactories.add(gatewayFactory);
            return this;
        }
        
        /**
         * Equivalent to calling {@code withGatewayFactory(() -> ServletGateway.create(DEFAULT_SERVLET)) }.
         * @return this builder
         */
        public Builder withServletGateway() {
            return withGatewayFactory(() -> ServletGateway.create(DEFAULT_SERVLET));
        }
        
        /**
         * Equivalent to calling {@code withGatewayFactory(() -> DirectGateway.create(DEFAULT_DIRECT)) }.
         * @return this builder
         */
        public Builder withDirectGateway() {
            return withGatewayFactory(() -> DirectGateway.create(DEFAULT_DIRECT));
        }

        /**
         * Equivalent to calling {@code withGatewayFactory(() -> TimerGateway.create(DEFAULT_TIMER)) }.
         * @return this builder
         */
        public Builder withTimerGateway() {
            return withGatewayFactory(() -> TimerGateway.create(DEFAULT_TIMER));
        }
        
        /**
         * Equivalent to calling {@code withGatewayFactory(() -> LogGateway.create(DEFAULT_LOG)) }.
         * @return this builder
         */
        public Builder withLogGateway() {
            return withGatewayFactory(() -> LogGateway.create(DEFAULT_LOG));
        }

        /**
         * Equivalent to calling {@code withActorGateway(Runtime.getRuntime().availableProcessors()) }.
         * @return this builder
         */
        public Builder withActorGateway() {
            return withActorGateway(Runtime.getRuntime().availableProcessors());
        }

        /**
         * Equivalent to calling {@code withActorGateway(concurrency, MemoryStore.create(DEFAULT_ACTOR, concurrency)) }.
         * @param concurrency number of threads for the actor gateway to use
         * @return this builder
         */
        public Builder withActorGateway(int concurrency) {
            return withActorGateway(concurrency, MemoryStore.create(DEFAULT_ACTOR, concurrency));
        }

        /**
         * Equivalent to calling {@code withGatewayFactory(() -> ActorGateway.create(DEFAULT_ACTOR, concurrency, store)) }.
         * @param concurrency number of threads for the actor gateway to use
         * @param store storage engine for storing/retrieving actors
         * @return this builder
         */
        public Builder withActorGateway(int concurrency, Store store) {
            return withGatewayFactory(() -> ActorGateway.create(DEFAULT_ACTOR, concurrency, store));
        }
        
        /**
         * Build the actor system.
         * @return new actor system
         * @throws RuntimeException on bad build parameters
         */
        public ActorSystem build() {
            return new ActorSystem(gatewayFactories);
        }
    }
}
