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
import com.offbynull.peernetic.router.MappedPort;
import com.offbynull.peernetic.router.PortMapException;
import com.offbynull.peernetic.router.PortMapperEventListener;
import com.offbynull.peernetic.router.PortType;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.lang3.Validate;

/**
 * A PCP {@link PortMapper} implementation.
 * @author Kasra Faghihi
 */
public final class PcpPortMapper extends PortMapper {
    private static final int ATTEMPTS = 4;
    
    private Random random;
    private PcpController controller;
    private Timer timer;
    private Lock lock;
    private MultiMap<TaskKey, CreateTask> createTasks; // used to notify when mappings are created
    private Map<TaskKey, NotifyCreateFailedTask> createFailedTasks; // used to notify when mappings weren't created
    private Map<TaskKey, RefreshTask> refreshTasks; // used to refresh mappings periodically
    private Map<TaskKey, NotifyRefreshFailedTask> deathTasks; // used to notify when mappings haven't been refreshed

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
        
        random = new Random();
        controller = new PcpController(random, gatewayAddress, selfAddress, new CustomPcpControllerListener());
        timer = new Timer("PCP Client Timer", true);
        createTasks = new MultiValueMap<>();
        createFailedTasks = new HashMap<>();
        refreshTasks = new HashMap<>();
        deathTasks = new HashMap<>();
        lock = new ReentrantLock();
    }
    
    @Override
    public void mapPort(PortType portType, int internalPort) {
        Validate.notNull(portType);
        Validate.inclusiveBetween(1, 65535, internalPort);
        
        lock.lock();
        try {
            if (isPortAlreadyBeingHandled(portType, internalPort)) {
                throw new PortMapException("Port already being handled");
            }

            for (int i = 0; i < ATTEMPTS; i++) {
                TaskKey key = new TaskKey(portType, internalPort);
                CreateTask createTask = new CreateTask(portType, internalPort);
                createTasks.put(key, createTask);
                
                // timeout duration should double each iteration, starting from 250 according to spec
                // i = 1, maxWaitTime = (1 << (1-1)) * 250 = (1 << 0) * 250 = 1 * 250 = 250
                // i = 2, maxWaitTime = (1 << (2-1)) * 250 = (1 << 1) * 250 = 2 * 250 = 500
                // i = 3, maxWaitTime = (1 << (3-1)) * 250 = (1 << 2) * 250 = 4 * 250 = 1000
                // i = 4, maxWaitTime = (1 << (4-1)) * 250 = (1 << 3) * 250 = 8 * 250 = 2000
                // ...
                timer.schedule(createTask, (1 << (i - 1)) * 250);
            }
        } catch (IllegalArgumentException | BufferUnderflowException e) {
            throw new PortMapException(e);
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public void unmapPort(PortType portType, int internalPort) {
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
            // ignore, in case this method gets called multiple times
//            if (!isPortAlreadyBeingHandled(portType, internalPort)) {
//                throw new PortMapException("Port not being handled");
//            }
            
            removeMappings(portType, internalPort);
            controller.requestMapOperationAsync(portType, internalPort, 0, anyExternalAddr, 0);
        } catch (IllegalArgumentException | BufferUnderflowException e) {
            throw new PortMapException(e);
        } finally {
            lock.unlock();
        }
    }
    
    private boolean isPortAlreadyBeingHandled(PortType portType, int internalPort) {
        lock.lock();
        try {
            TaskKey key = new TaskKey(portType, internalPort);
            return createTasks.containsKey(key) || refreshTasks.containsKey(key) || deathTasks.containsKey(key);
        } finally {
            lock.unlock();
        }
    }

    private void removeMappings(PortType portType, int internalPort) {
        lock.lock();
        try {
            TaskKey key = new TaskKey(portType, internalPort);
            
            Collection<CreateTask> createTaskItems = (Collection<CreateTask>) createTasks.remove(key);
            if (createTasks != null) {
                for (CreateTask task : createTaskItems) {
                    task.cancel();
                }
            }
            NotifyCreateFailedTask createFailedTask = createFailedTasks.get(key);
            if (createFailedTask != null) {
                createFailedTask.cancel();
            }
            RefreshTask refreshTask = refreshTasks.remove(key);
            if (refreshTask != null) {
                refreshTask.cancel();
            }
            NotifyRefreshFailedTask deathTask = deathTasks.remove(key);
            if (deathTask != null) {
                deathTask.cancel();
            }
        } finally {
            lock.unlock();
        }        
    }

    private boolean removeCreateMappings(PortType portType, int internalPort) {
        lock.lock();
        try {
            TaskKey key = new TaskKey(portType, internalPort);
            
            boolean found = false;
            Collection<CreateTask> createTaskItems = (Collection<CreateTask>) createTasks.remove(key);
            if (createTasks != null) {
                for (CreateTask task : createTaskItems) {
                    task.cancel();
                }
                found = true;
            }
            NotifyCreateFailedTask createFailedTask = createFailedTasks.get(key);
            if (createFailedTask != null) {
                createFailedTask.cancel();
                found = true;
            }
            return found;
        } finally {
            lock.unlock();
        }        
    }
    
    private MappedPort removeRefreshMappings(PortType portType, int internalPort) {
        lock.lock();
        try {
            TaskKey key = new TaskKey(portType, internalPort);
    
            MappedPort mappedPort = null;
            RefreshTask refreshTask = refreshTasks.remove(key);
            if (refreshTask != null) {
                refreshTask.cancel();
                mappedPort = refreshTask.getPortDetails();
            }
            NotifyRefreshFailedTask deathTask = deathTasks.remove(key);
            if (deathTask != null) {
                deathTask.cancel();
            }
            
            return mappedPort;
        } finally {
            lock.unlock();
        }
    }

    private void addRefreshMappings(MappedPort portDetails, int lifetime) {
        lock.lock();
        try {
            TaskKey key = new TaskKey(portDetails.getPortType(), portDetails.getInternalPort());

            NotifyRefreshFailedTask notifyDeathTask = new NotifyRefreshFailedTask(portDetails);
            deathTasks.put(key, notifyDeathTask);
            timer.schedule(notifyDeathTask, lifetime);

            RefreshTask refreshTask = new RefreshTask(portDetails);
            refreshTasks.put(key, refreshTask);
            timer.schedule(refreshTask, random.nextInt((int) (lifetime / 4L)));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        timer.cancel();
        timer.purge();
        controller.close();
    }
    
    private class CreateTask extends TimerTask {

        private PortType portType;
        private int internalPort;

        public CreateTask(PortType portType, int internalPort) {
            Validate.notNull(portType);
            Validate.inclusiveBetween(1, 65535, internalPort);
            this.portType = portType;
            this.internalPort = internalPort;
        }
        
        @Override
        public void run() {
            InetAddress anyAddress;
            try {
                anyAddress = InetAddress.getByName("::");
            } catch (UnknownHostException uhe) { // should never happen
                throw new IllegalStateException(uhe);
            }
            
            controller.requestMapOperationAsync(portType, internalPort, 0, anyAddress, 3600L);
        }
    }

    private class NotifyCreateFailedTask extends TimerTask {

        private PortType portType;
        private int internalPort;

        public NotifyCreateFailedTask(PortType portType, int internalPort) {
            Validate.notNull(portType);
            Validate.inclusiveBetween(1, 65535, internalPort);
            this.portType = portType;
            this.internalPort = internalPort;
        }
        
        @Override
        public void run() {
            removeMappings(portType, internalPort);

            try {
                getListener().mappingCreationFailed(portType, internalPort);
            } catch (RuntimeException re) { // NOPMD
                // do nothing
            }
        }
    }

    private class RefreshTask extends TimerTask {

        private MappedPort portDetails;

        public RefreshTask(MappedPort portDetails) {
            Validate.notNull(portDetails);
            this.portDetails = portDetails;
        }
        
        @Override
        public void run() {
            // trigger restart
            controller.requestMapOperationAsync(portDetails.getPortType(),
                    portDetails.getInternalPort(),
                    portDetails.getExternalPort(),
                    portDetails.getExternalAddress(),
                    3600L, new PreferFailurePcpOption());
        }

        public MappedPort getPortDetails() {
            return portDetails;
        }
        
    }

    private class NotifyRefreshFailedTask extends TimerTask {
        
        private MappedPort portDetails;

        public NotifyRefreshFailedTask(MappedPort portDetails) {
            Validate.notNull(portDetails);
            this.portDetails = portDetails;
        }

        @Override
        public void run() {
            removeMappings(portDetails.getPortType(), portDetails.getInternalPort());

            try {
                getListener().mappingLost(portDetails);
            } catch (RuntimeException re) { // NOPMD
                // do nothing
            }
        }
    }

    private class CustomPcpControllerListener implements PcpControllerListener {

        @Override
        public void incomingResponse(CommunicationType type, PcpResponse response) {
            if (response instanceof AnnouncePcpResponse) {
                lock.lock();
                try {
                    
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
                
                MappedPort oldPortDetails = null;
                MappedPort newPortDetails = new MappedPort(mapPcpResponse.getInternalPort(), mapPcpResponse.getAssignedExternalPort(),
                        mapPcpResponse.getAssignedExternalIpAddress(), portType);
                
                lock.lock();
                try {
                    if (removeCreateMappings(portType, mapPcpResponse.getInternalPort())) {
                        // add refresh tasks
                        int newLifetime = (int) Math.min(Integer.MAX_VALUE, mapPcpResponse.getLifetime());
                        addRefreshMappings(newPortDetails, newLifetime);                        
                    } else if ((oldPortDetails = removeRefreshMappings(portType, mapPcpResponse.getInternalPort())) != null) {
                        // re-add refresh tasks
                        int newLifetime = (int) Math.min(Integer.MAX_VALUE, mapPcpResponse.getLifetime());
                        addRefreshMappings(newPortDetails, newLifetime);
                    }
                } finally {
                    lock.unlock();
                }
                
                // if refreshed, compare if port mappings are different and notify if they are
                if (!newPortDetails.equals(oldPortDetails)) {
                    try {
                        getListener().mappingChanged(oldPortDetails, newPortDetails);
                    } catch (RuntimeException re) { // NOPMD
                        // do nothing
                    }
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
}
