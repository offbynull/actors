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
package com.offbynull.peernetic.router.pcp;

import com.offbynull.peernetic.router.PortMapper;
import com.offbynull.peernetic.router.PortMapException;
import com.offbynull.peernetic.router.PortMapperEventListener;
import com.offbynull.peernetic.router.PortType;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

/**
 * A PCP {@link PortMapper} implementation.
 * @author Kasra Faghihi
 */
public final class PcpPortMapper extends PortMapper {
    private static final int ATTEMPTS = 4;
    
    private PcpController controller;
    private ScheduledExecutorService timer;
    private Lock lock;
    private Map<TaskKey, TaskValue> tasks; // used to notify when mappings are created

    /**
     * Constructs a {@link PcpPortMapper} object.
     * @param gatewayAddress address of router/gateway
     * @param selfAddress address of the interface that can talk to the router/gateway
     * @param listener event listener
     * @throws NullPointerException if any argument is {@code null}.
     */
    public PcpPortMapper(InetAddress gatewayAddress, InetAddress selfAddress, PortMapperEventListener listener) {
        super(listener);
        
        Validate.notNull(gatewayAddress);
        Validate.notNull(selfAddress);
        Validate.notNull(listener);
        
        controller = new PcpController(new Random(), gatewayAddress, selfAddress, new CustomPcpControllerListener());
        timer = new ScheduledThreadPoolExecutor(1,
                new BasicThreadFactory.Builder().namingPattern("PCP Task Scheduler").daemon(true).build());
        tasks = new HashMap<>();
        lock = new ReentrantLock();
    }
    
    @Override
    public void mapPort(final PortType portType, final int internalPort) throws InterruptedException {
        Validate.notNull(portType);
        Validate.inclusiveBetween(1, 65535, internalPort);
        
        lock.lock();
        try {
            Validate.validState(!timer.isShutdown());
            
            if (isPortAlreadyBeingHandled(portType, internalPort)) {
                throw new PortMapException("Port already being handled");
            }

            MapPcpResponse response = controller.requestMapOperation(ATTEMPTS, portType, internalPort, 0, InetAddress.getByName("::"),
                    3600);
            
            TaskKey key = new TaskKey(portType, internalPort);
            MaintainCallable maintainCallable = MaintainCallable.createAndScheduler(timer, controller, portType, internalPort,
                    response.getAssignedExternalPort(), response.getAssignedExternalIpAddress(), response.getLifetime());
            NotifyStaleCallable notifyStaleCallable = NotifyStaleCallable.createAndScheduler(timer, getListener(), response.getLifetime(),
                    internalPort);
            
            tasks.put(key, new TaskValue(maintainCallable, notifyStaleCallable));
        } catch (IllegalArgumentException | BufferUnderflowException | UnknownHostException e) {
            throw new PortMapException(e);
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public void unmapPort(PortType portType, int internalPort) throws InterruptedException {
        Validate.notNull(portType);
        Validate.inclusiveBetween(1, 65535, internalPort);
        
        InetAddress anyExternalAddr;
        try {
            anyExternalAddr = InetAddress.getByName("::");
        } catch (UnknownHostException uhe) { // should never happen
            throw new IllegalStateException(uhe);
        }

        lock.lock();
        try {
            Validate.validState(!timer.isShutdown());
            
            if (!isPortAlreadyBeingHandled(portType, internalPort)) {
                throw new PortMapException("Port not being handled");
            }
            
            try {
                controller.requestMapOperation(ATTEMPTS, portType, internalPort, 0, anyExternalAddr, 0);
            } catch (IllegalArgumentException | BufferUnderflowException e) {
                throw new PortMapException(e);
            } finally {
                TaskKey key = new TaskKey(portType, internalPort);
                TaskValue value = tasks.remove(key);
                
                if (value != null) {
                    value.getMaintain().cancel();
                    value.getNotifyStale().cancel();
                }
            }
        } finally {
            lock.unlock();
        }
    }
    
    private boolean isPortAlreadyBeingHandled(PortType portType, int internalPort) {
        lock.lock();
        try {
            TaskKey key = new TaskKey(portType, internalPort);
            return tasks.containsKey(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        lock.lock();
        try {
            if (timer.isShutdown()) {
                return;
            }

            controller.close();
            timer.shutdownNow();
        } finally {
            lock.unlock();
        }
    }
    
    private static final class MaintainCallable implements Callable<Void> {

        private ScheduledExecutorService scheduler;
        private PcpController controller;
        private PortType portType;
        private int internalPort;
        private int preferredExternalPort;
        private InetAddress preferredExternalAddress;
        private volatile int duration;
        private volatile ScheduledFuture<Void> selfFuture;
                
        public static MaintainCallable createAndScheduler(ScheduledExecutorService scheduler, PcpController controller, PortType portType,
                int internalPort, int preferredExternalPort, InetAddress preferredExternalAddress, long duration) {
            MaintainCallable callable = new MaintainCallable(scheduler, controller, portType, internalPort, preferredExternalPort,
                    preferredExternalAddress, duration);
            callable.selfFuture = scheduler.schedule(callable, callable.duration, TimeUnit.SECONDS);
            return callable;
        }
        
        public MaintainCallable(ScheduledExecutorService scheduler, PcpController controller, PortType portType, int internalPort,
                int preferredExternalPort, InetAddress preferredExternalAddress, long duration) {
        
            Validate.notNull(scheduler);
            Validate.notNull(controller);
            Validate.notNull(portType);
            Validate.inclusiveBetween(1, 65535, internalPort);
            Validate.inclusiveBetween(1, 65535, preferredExternalPort);
            Validate.notNull(preferredExternalAddress);
            Validate.inclusiveBetween(0L, Long.MAX_VALUE, duration);
            this.controller = controller;
            this.portType = portType;
            this.internalPort = internalPort;
            this.preferredExternalPort = preferredExternalPort;
            this.preferredExternalAddress = preferredExternalAddress;
            this.scheduler = scheduler;
            this.duration = (int) Math.min(Integer.MAX_VALUE, duration / (long) ATTEMPTS);
        }

        public void reset(long duration) {
            Validate.inclusiveBetween(0L, Long.MAX_VALUE, duration);
            this.duration = (int) Math.min(Integer.MAX_VALUE, duration / (long) ATTEMPTS);
            selfFuture = scheduler.schedule(this, duration, TimeUnit.SECONDS);
        }

        public void cancel() {
            selfFuture.cancel(false);
        }

        public int getPreferredExternalPort() {
            return preferredExternalPort;
        }

        public InetAddress getPreferredExternalAddress() {
            return preferredExternalAddress;
        }
        
        @Override
        public Void call() throws Exception {
            controller.requestMapOperationAsync(portType,
                    internalPort,
                    preferredExternalPort,
                    preferredExternalAddress,
                    3600L);

            selfFuture = scheduler.schedule(this, duration, TimeUnit.SECONDS);
            
            return null;
        }
    }

    private static final class NotifyStaleCallable implements Callable<Void> {

        private ScheduledExecutorService scheduler;
        private PortMapperEventListener listener;
        private int internalPort;
        private volatile ScheduledFuture<Void> selfFuture;
                
        public static NotifyStaleCallable createAndScheduler(ScheduledExecutorService scheduler, PortMapperEventListener listener,
                long duration, int internalPort) {
            NotifyStaleCallable callable = new NotifyStaleCallable(scheduler, listener, internalPort);
            scheduler.schedule(callable, duration, TimeUnit.SECONDS);
            
            return callable;
        }
        
        private NotifyStaleCallable(ScheduledExecutorService scheduler, PortMapperEventListener listener, int internalPort) {
            Validate.notNull(scheduler);
            Validate.notNull(listener);
            Validate.inclusiveBetween(1, 65535, internalPort);
            this.scheduler = scheduler;
            this.listener = listener;
            this.internalPort = internalPort;
        }

        public void reset(long duration) {
            Validate.inclusiveBetween(0L, Long.MAX_VALUE, duration);
            int revisedDuration = (int) Math.min(Integer.MAX_VALUE, duration / (long) ATTEMPTS);
            selfFuture.cancel(false);
            selfFuture = scheduler.schedule(this, revisedDuration, TimeUnit.SECONDS);
        }

        public void cancel() {
            selfFuture.cancel(false);
        }
        
        @Override
        public Void call() throws Exception {
            listener.resetRequired("PCP server did not respond to MAP requests for internal port " + internalPort);
            return null;
        }
    }

    private static final class DeferredNotifyCallable implements Callable<Void> {

        private PortMapperEventListener listener;
        private String message;
                
        public static DeferredNotifyCallable createAndScheduleImmediate(ScheduledExecutorService scheduler,
                PortMapperEventListener listener, String message) {
            DeferredNotifyCallable callable = new DeferredNotifyCallable(listener, message);
            scheduler.schedule(callable, 1, TimeUnit.SECONDS);
            return callable;
        }
        
        private DeferredNotifyCallable(PortMapperEventListener listener, String message) {
            Validate.notNull(listener);
            Validate.notNull(message);
            this.listener = listener;
            this.message = message;
        }
        
        @Override
        public Void call() throws Exception {
            listener.resetRequired(message);
            return null;
        }
    }
    
    private class CustomPcpControllerListener implements PcpControllerListener {

        @Override
        public void incomingResponse(CommunicationType type, PcpResponse response) {
            if (response instanceof AnnouncePcpResponse) {
                // re-send mapping requests
                lock.lock();
                try {
                    Validate.validState(!timer.isShutdown());
                    DeferredNotifyCallable.createAndScheduleImmediate(timer, getListener(), "PCP server sent out an ANNOUNCE response");
                } finally {
                    lock.unlock();
                }                
            } else if (response instanceof MapPcpResponse) {
                // construct new port details object
                MapPcpResponse mapPcpResponse = (MapPcpResponse) response;
                
                PortType portType;
                
                if (mapPcpResponse.getProtocol() == PortType.TCP.getProtocolNumber()) {
                    portType = PortType.TCP;
                } else if (mapPcpResponse.getProtocol() == PortType.UDP.getProtocolNumber()) {
                    portType = PortType.UDP;
                } else {
                    throw new IllegalArgumentException();
                }
                
                lock.lock();
                try {
                    Validate.validState(!timer.isShutdown());
                    
                    TaskKey key = new TaskKey(portType, mapPcpResponse.getInternalPort());
                    TaskValue value = tasks.get(key);
                    if (value == null) {
                        return;
                    }
                    
                    int oldExternalPort = value.getMaintain().getPreferredExternalPort();
                    InetAddress oldExternalAddress = value.getMaintain().getPreferredExternalAddress();
                    
                    // if refreshed, compare if port mappings are different and notify if they are
                    if (oldExternalPort != mapPcpResponse.getAssignedExternalPort()
                            || !oldExternalAddress.equals(mapPcpResponse.getAssignedExternalIpAddress())) {
                        DeferredNotifyCallable.createAndScheduleImmediate(timer, getListener(),
                                "PCP server changed external IP/port for internal port " + mapPcpResponse.getInternalPort());
                    } else {
                        value.getNotifyStale().reset(mapPcpResponse.getLifetime());
                        value.getMaintain().reset(mapPcpResponse.getLifetime());
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    private static final class TaskKey {
        private PortType portType;
        private int internalPort;

        public TaskKey(PortType portType, int internalPort) {
            Validate.notNull(portType);
            Validate.inclusiveBetween(1, 65535, internalPort);
            this.portType = portType;
            this.internalPort = internalPort;
        }

        public PortType getPortType() {
            return portType;
        }

        public int getInternalPort() {
            return internalPort;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 83 * hash + Objects.hashCode(this.portType);
            hash = 83 * hash + this.internalPort;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TaskKey other = (TaskKey) obj;
            if (this.portType != other.portType) {
                return false;
            }
            if (this.internalPort != other.internalPort) {
                return false;
            }
            return true;
        }
        
    }
    
    private static final class TaskValue {
        private MaintainCallable maintain;
        private NotifyStaleCallable notifyStale;

        public TaskValue(MaintainCallable maintain, NotifyStaleCallable notifyStale) {
            Validate.notNull(maintain);
            Validate.notNull(notifyStale);

            this.maintain = maintain;
            this.notifyStale = notifyStale;
        }

        public MaintainCallable getMaintain() {
            return maintain;
        }

        public NotifyStaleCallable getNotifyStale() {
            return notifyStale;
        }

    }
}
