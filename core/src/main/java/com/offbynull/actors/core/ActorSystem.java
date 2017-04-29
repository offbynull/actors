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
package com.offbynull.actors.core;

import com.offbynull.actors.core.actor.ActorRunner;
import com.offbynull.actors.core.checkpoint.NullCheckpointer;
import static com.offbynull.actors.core.common.DefaultAddresses.DEFAULT_DIRECT;
import static com.offbynull.actors.core.common.DefaultAddresses.DEFAULT_LOG;
import static com.offbynull.actors.core.common.DefaultAddresses.DEFAULT_RUNNER;
import static com.offbynull.actors.core.common.DefaultAddresses.DEFAULT_TIMER;
import com.offbynull.actors.core.gateway.Gateway;
import com.offbynull.actors.core.gateways.direct.DirectGateway;
import com.offbynull.actors.core.gateways.log.LogGateway;
import com.offbynull.actors.core.gateways.timer.TimerGateway;
import com.offbynull.actors.core.shuttle.Shuttle;
import com.offbynull.coroutines.user.Coroutine;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.commons.lang3.tuple.ImmutablePair;
import com.offbynull.actors.core.checkpoint.Checkpointer;

/**
 * Actor system.
 * <p>
 * Use {@link #builder() } to construct and start an actor system.
 * @author Kasra Faghihi
 */
public final class ActorSystem implements AutoCloseable {
    private final ActorRunner runner;
    private final Checkpointer checkpointer;
    
    private final Set<Gateway> gateways;
    
    private final DirectGateway directGateway;

    private ActorSystem(
            Map<String, ImmutablePair<Coroutine, Object[]>> actors,
            List<Supplier<Gateway>> gatewayFactories,
            String runnerName,
            int runnerCores,
            Checkpointer runnerCheckpointer) {
        ActorRunner runner = null;
        DirectGateway directGateway = null;

        gateways = new HashSet<>();
        
        try {
            this.checkpointer = runnerCheckpointer;
            
            for (Supplier<Gateway> gatewayFactory : gatewayFactories) {
                gateways.add(gatewayFactory.get());
            }
        
            runner = ActorRunner.create(runnerName, runnerCores, runnerCheckpointer);
            
            directGateway = DirectGateway.create(DEFAULT_DIRECT);
            gateways.add(TimerGateway.create(DEFAULT_TIMER));
            gateways.add(LogGateway.create(DEFAULT_LOG));
            gateways.add(directGateway);

            for (Gateway gateway : gateways) {
                bindGatewayToOthers(gateway, gateways, runner);
            }

            for (Entry<String, ImmutablePair<Coroutine, Object[]>> entry : actors.entrySet()) {
                String id = entry.getKey();
                Coroutine actor = entry.getValue().left;
                Object[] primingMessages = entry.getValue().right;

                runner.addActor(id, actor, primingMessages);
            }
        } catch (RuntimeException re) {
            if (runnerCheckpointer != null) {
                try {
                    runnerCheckpointer.close();
                } catch (Exception e) {
                    // do nothing
                }
            }

            if (runner != null) {
                try {
                    runner.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
            
            for (Gateway gateway : gateways) {
                if (gateway != null) {
                    try {
                        gateway.close();
                    } catch (Exception e) {
                        // do nothing
                    }
                }
            }
            
            throw re;
        }
        
        this.runner = runner;
        this.directGateway = directGateway;
    }
    
    private static void bindGatewayToOthers(Gateway gateway, Set<Gateway> allGateways, ActorRunner runner) {
        // Add gateway to runner+gateways
        Shuttle gatewayShuttle = gateway.getIncomingShuttle();
        runner.addOutgoingShuttle(gatewayShuttle);
        for (Gateway otherGateway : allGateways) {
            otherGateway.addOutgoingShuttle(gatewayShuttle);
        }

        // Add runner to gateway
        Shuttle runnerShuttle = runner.getIncomingShuttle();
        gateway.addOutgoingShuttle(runnerShuttle);
    }

    /**
     * Get the direct gateway used for outside communication with this actor system.
     * @return direct gateway
     */
    public DirectGateway getDirectGateway() {
        return directGateway;
    }
    
    @Override
    public void close() {
        if (checkpointer != null) {
            try {
                checkpointer.close();
            } catch (Exception e) {
                // do nothing
            }
        }

        if (runner != null) {
            try {
                runner.close();
            } catch (Exception e) {
                // do nothing
            }
        }

        for (Gateway gateway : gateways) {
            if (gateway != null) {
                try {
                    gateway.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
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
     * Actor system builder.
     */
    public static final class Builder {
        
        private LinkedHashMap<String, ImmutablePair<Coroutine, Object[]>> actors;
        private List<Supplier<Gateway>> gatewayFactories;
        private String runnerName;
        private int runnerCores;
        private Checkpointer runnerCheckpointer;
        
        private Builder() {
            actors = new LinkedHashMap<>();
            gatewayFactories = new ArrayList<>();
            runnerName = DEFAULT_RUNNER;
            runnerCores = Runtime.getRuntime().availableProcessors();
            runnerCheckpointer = new NullCheckpointer();
        }
        
        /**
         * Add gateway to new actor system being built.
         * <p>
         * If it all possible, use {@link #withGatewayFactory(java.util.function.Supplier) } instead.
         * @param gateway gateway
         * @return this builder
         * @see #withGatewayFactory(java.util.function.Supplier) 
         */
        public Builder withGateway(Gateway gateway) {
            gatewayFactories.add(() -> gateway);
            return this;
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
         * Add actor to new actor system being built.
         * @param id id of actor
         * @param actor actor
         * @param primingMessages priming message
         * @see ActorRunner#addActor(java.lang.String, com.offbynull.coroutines.user.Coroutine, java.lang.Object...) 
         * @return this builder
         */
        public Builder withActor(String id, Coroutine actor, Object ... primingMessages) {
            actors.put(id, ImmutablePair.of(actor, primingMessages));
            return this;
        }
        
        /**
         * Name of actor runner to use.
         * @param name runner name
         * @return this builder
         */
        public Builder withRunnerName(String name) {
            this.runnerName = name;
            return this;
        }
        
        /**
         * Number of cores to use.
         * @param cores core count
         * @return this builder
         */
        public Builder withRunnerCoreCount(int cores) {
            this.runnerCores = cores;
            return this;
        }
        
        /**
         * Checkpointing backend to use for actors.
         * @param checkpointer checkpointer
         * @return this builder
         */
        public Builder withRunnerCheckpointer(Checkpointer checkpointer) {
            this.runnerCheckpointer = checkpointer;
            return this;
        }
        
        /**
         * Build the actor system.
         * @return new actor system
         * @throws RuntimeException on bad build parameters
         */
        public ActorSystem build() {
            return new ActorSystem(actors, gatewayFactories, runnerName, runnerCores, runnerCheckpointer);
        }
    }
}
