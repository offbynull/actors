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

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.SettableFuture;
import com.offbynull.peernetic.router.MappedPort;
import com.offbynull.peernetic.router.Port;
import com.offbynull.peernetic.router.PortMapException;
import com.offbynull.peernetic.router.PortMapperEventListener;
import com.offbynull.peernetic.router.PortType;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;

final class PcpPortMapperService extends AbstractExecutionThreadService {
    private static final int MAX_ATTEMPTS = 4;
    public static final int DEFAULT_LIFETIME = 60;

    private PortMapperEventListener portMapperListener;
    private LinkedBlockingQueue<Object> msgQueue;
    private PcpController controller;

    private Map<Port, PortState> portStates;
    private PriorityQueue<PortEvent> portEvents;

    public PcpPortMapperService(LinkedBlockingQueue<Object> msgQueue, PortMapperEventListener portMapperListener, InetAddress gatewayAddress,
            InetAddress selfAddress) {
        Validate.notNull(portMapperListener);
        Validate.notNull(msgQueue);
        Validate.notNull(gatewayAddress);
        Validate.notNull(selfAddress);

        this.portMapperListener = portMapperListener;
        this.msgQueue = msgQueue;
        controller = new PcpController(new Random(), gatewayAddress, selfAddress, new ServicePcpControllerListener(msgQueue));
    }

    @Override
    protected void startUp() throws Exception {
        portStates = new HashMap<>();
        portEvents = new PriorityQueue<>(11, new PortEventComparator());
    }

    @Override
    protected void run() throws Exception {
        while (true) {
            long waitDuration = getWaitDuration();
            Object nextMsg = msgQueue.poll(waitDuration, TimeUnit.MILLISECONDS);

            long currentTime = System.currentTimeMillis();

            handleTimeouts(currentTime);

            if (nextMsg == null) {
                continue;
            }

            if (handleMessage(nextMsg)) {
                return;
            }
        }
    }

    private boolean handleMessage(Object nextMsg) throws UnknownHostException {
        if (nextMsg instanceof StopMessage) {
            return true;
        } else if (nextMsg instanceof MapPortMessage) {
            MapPortMessage mpm = (MapPortMessage) nextMsg;
            handleMapPortMessage(mpm);
        } else if (nextMsg instanceof UnmapPortMessage) {
            UnmapPortMessage upm = (UnmapPortMessage) nextMsg;
            handleUnmapPortMessage(upm);
        } else if (nextMsg instanceof PcpResponseMessage) {
            PcpResponseMessage prm = (PcpResponseMessage) nextMsg;
            handlePcpResponseMessage(prm);
        } else {
            throw new IllegalStateException();
        }
        return false;
    }

    private void handlePcpResponseMessage(PcpResponseMessage prm) {
        PcpResponse response = prm.getResponse();
        if (response instanceof AnnouncePcpResponse) {
            portMapperListener.resetRequired("Gateway sent ANNOUNCE notification. Mappings may have been lost.");
        } else if (response instanceof MapPcpResponse) {
            // generate port from message
            MapPcpResponse mapPcpResponse = (MapPcpResponse) response;
            int portNumber = mapPcpResponse.getInternalPort();
            PortType portType;
            if (mapPcpResponse.getProtocol() == PortType.TCP.getProtocolNumber()) {
                portType = PortType.TCP;
            } else if (mapPcpResponse.getProtocol() == PortType.UDP.getProtocolNumber()) {
                portType = PortType.UDP;
            } else {
                return; // ignore
            }
            Port internalPort = new Port(portType, portNumber);
            
            // handle state
            PortState state = portStates.get(internalPort);
            if (state instanceof CreatePortState) {
                CreatePortState cps = (CreatePortState) state;
                MapPcpRequest mapPcpRequest = cps.getRequest();
                if (!mapPcpRequest.getMappingNonce().equals(mapPcpResponse.getMappingNonce())) {
                    cps.getFuture().setException(
                            new PortMapException("Unable to map. Incorrect nonce encountered. Use different internal port."));
                    return;
                }
                replaceStateWithMaintainPortState(internalPort, mapPcpRequest, mapPcpResponse.getAssignedExternalPort(),
                        mapPcpResponse.getAssignedExternalIpAddress(),
                        TimeUnit.SECONDS.toMillis(mapPcpResponse.getLifetime()));
                MappedPort mappedPort = new MappedPort(internalPort.getPortNumber(), mapPcpResponse.getAssignedExternalPort(),
                        mapPcpResponse.getAssignedExternalIpAddress(), internalPort.getPortType());
                cps.getFuture().set(mappedPort);
            } else if (state instanceof DestroyPortState) {
                DestroyPortState dps = (DestroyPortState) state;
                MapPcpRequest mapPcpRequest = dps.getRequest();
                if (!mapPcpRequest.getMappingNonce().equals(mapPcpResponse.getMappingNonce())) {
                    // mapping nonce doesn't match, ignore
                    return;
                }
                removeState(internalPort);
                dps.getFuture().set(null);
            } else if (state instanceof MaintainPortState) {
                MaintainPortState mps = (MaintainPortState) state;
                MapPcpRequest mapPcpRequest = mps.getRequest();
                if (mps.getExternalPort() != mapPcpResponse.getAssignedExternalPort()
                        || !mps.getExternalAddress().equals(mapPcpResponse.getAssignedExternalIpAddress())) {
                    // external port/ip changed, notify and ignore
                    portMapperListener.resetRequired("Mapping for " + internalPort + " changed.");
                    return;
                }
                replaceStateWithMaintainPortState(internalPort, mapPcpRequest, mapPcpResponse.getAssignedExternalPort(),
                        mapPcpResponse.getAssignedExternalIpAddress(),
                        TimeUnit.SECONDS.toMillis(mapPcpResponse.getLifetime()));
            }
        }
    }

    private void removeState(Port internalPort) {
        PortState oldPortState = portStates.remove(internalPort);
        if (oldPortState != null) {
            for (PortEvent remainingEvent : oldPortState.viewEvents()) {
                remainingEvent.cancel();
            }
        }
    }
    
    private void replaceStateWithMaintainPortState(Port internalPort, MapPcpRequest mapPcpRequest, int externalPort,
            InetAddress externalAddress, long lifetime) {
        PortState oldPortState = portStates.remove(internalPort);
        if (oldPortState != null) {
            for (PortEvent remainingEvent : oldPortState.viewEvents()) {
                remainingEvent.cancel();
            }
        }
        
        if (lifetime == 0L) {
            portMapperListener.resetRequired("PCP server returned MAP lifetime of 0.");
            return;
        }
        
        int maintainDuration = (int) Math.min(Integer.MAX_VALUE, lifetime / 4);
        if (maintainDuration == 0) {
            portMapperListener.resetRequired("PCP server returned MAP lifetime of " + maintainDuration + ", which is too small to use.");
            return;
        }
        
        MaintainPortState mps = new MaintainPortState(internalPort, mapPcpRequest, externalPort, externalAddress);
        
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            long wait = maintainDuration * (i + 1);
            long retryTime = System.currentTimeMillis() + wait;
            PortEvent mpsEvent = new PortEvent(internalPort, retryTime);
            mps.addEvent(mpsEvent);
            portEvents.add(mpsEvent);
        }
        
        portStates.put(internalPort, mps);
    }

    private void handleUnmapPortMessage(UnmapPortMessage upm) throws UnknownHostException {
        PortState portState = portStates.get(upm.getInternalPort());
        if (portState == null) {
            upm.getFuture().setException(new PortMapException("Port has not been acquired"));
        } else if (portState instanceof CreatePortState) {
            upm.getFuture().setException(new PortMapException("Port is in the process of being acquired"));
        } else if (portState instanceof DestroyPortState) {
            upm.getFuture().setException(new PortMapException("Port is in the process of being released"));
        }
        
        Port internalPort = upm.getInternalPort();
        MapPcpRequest mapPcpRequest = controller.requestMapOperationAsync(internalPort.getPortType(), internalPort.getPortNumber(), 0,
                InetAddress.getByName("::"), 0);
        
        replaceStateWithDestoryPortState(internalPort, mapPcpRequest, upm.getFuture());
    }

    private void replaceStateWithDestoryPortState(Port internalPort, MapPcpRequest mapPcpRequest, SettableFuture<Void> future) {
        PortState oldPortState = portStates.remove(internalPort);
        if (oldPortState != null) {
            for (PortEvent remainingEvent : oldPortState.viewEvents()) {
                remainingEvent.cancel();
            }
        }
        
        DestroyPortState dps = new DestroyPortState(internalPort, mapPcpRequest, future);
        long retryTime = System.currentTimeMillis() + PcpUtils.getPcpWaitTime(1);
        PortEvent newEvent = new PortEvent(internalPort, retryTime);
        dps.addEvent(newEvent);
        portStates.put(internalPort, dps);
        portEvents.add(newEvent);
    }

    private void handleMapPortMessage(MapPortMessage mpm) throws UnknownHostException {
        PortState portState = portStates.get(mpm.getInternalPort());
        if (portState instanceof CreatePortState) {
            mpm.getFuture().setException(new PortMapException("Port is in the process of being acquired"));
        } else if (portState instanceof DestroyPortState) {
            mpm.getFuture().setException(new PortMapException("Port is in the process of being released"));
        } else if (portState instanceof MaintainPortState) {
            mpm.getFuture().setException(new PortMapException("Port is already acquired"));
        }
        
        Port internalPort = mpm.getInternalPort();
        MapPcpRequest mapPcpRequest = controller.requestMapOperationAsync(internalPort.getPortType(), internalPort.getPortNumber(), 0,
                InetAddress.getByName("::"), DEFAULT_LIFETIME);
        
        replaceStateWithCreatePortState(internalPort, mapPcpRequest, mpm.getFuture());
    }
    
    private void replaceStateWithCreatePortState(Port internalPort, MapPcpRequest mapPcpRequest, SettableFuture<MappedPort> future) {
        PortState oldPortState = portStates.remove(internalPort);
        if (oldPortState != null) {
            for (PortEvent remainingEvent : oldPortState.viewEvents()) {
                remainingEvent.cancel();
            }
        }
        
        CreatePortState cps = new CreatePortState(internalPort, mapPcpRequest, future);
        long retryTime = System.currentTimeMillis() + PcpUtils.getPcpWaitTime(1);
        PortEvent newEvent = new PortEvent(internalPort, retryTime);
        cps.addEvent(newEvent);
        portStates.put(internalPort, cps);
        portEvents.add(newEvent);
    }

    private long getWaitDuration() {
        PortEvent event = portEvents.peek();
        long waitUntil = event == null ? Long.MAX_VALUE : event.getHitTime();
        long waitDuration = Math.max(0L, waitUntil - System.currentTimeMillis());
        return waitDuration;
    }

    private void handleTimeouts(long currentTime) throws UnknownHostException {
        while (!portEvents.isEmpty() && currentTime >= portEvents.peek().getHitTime()) {
            PortEvent event = portEvents.poll(); // remove
            
            if (event.isCancelled()) {
                continue;
            }

            Port internalPort = event.getInternalPort();
            PortState state = portStates.get(internalPort);

            if (state instanceof MaintainPortState) {
                MaintainPortState mps = (MaintainPortState) state;
                mps.removeEvent(event);

                if (mps.getAttempt() == MAX_ATTEMPTS) {
                    removeState(internalPort);
                    portMapperListener.resetRequired("Mapping for " + internalPort + " lost.");
                } else {
                    mps.incrementAttempt();
                    MapPcpRequest mapPcpRequest = controller.requestMapOperationAsync(internalPort.getPortType(),
                            internalPort.getPortNumber(), 0, InetAddress.getByName("::"), DEFAULT_LIFETIME);
                    mps.setRequest(mapPcpRequest);
                }
            } else if (state instanceof CreatePortState) {
                CreatePortState cps = (CreatePortState) state;
                cps.removeEvent(event);
                
                if (cps.getAttempt() == MAX_ATTEMPTS) {
                    removeState(internalPort);
                    cps.getFuture().setException(new PortMapException("Unable to map"));
                } else {
                    cps.incrementAttempt();
                    MapPcpRequest mapPcpRequest = controller.requestMapOperationAsync(internalPort.getPortType(),
                            internalPort.getPortNumber(), 0, InetAddress.getByName("::"), DEFAULT_LIFETIME);
                    cps.setRequest(mapPcpRequest);
                    
                    long hitTime = System.currentTimeMillis() + PcpUtils.getPcpWaitTime(cps.getAttempt() + 1);
                    PortEvent newEvent = new PortEvent(cps.getInternalPort(), hitTime);
                    cps.addEvent(newEvent);
                    portEvents.add(newEvent);
                }
            } else if (state instanceof DestroyPortState) {
                DestroyPortState dps = (DestroyPortState) state;
                dps.removeEvent(event);

                if (dps.getAttempt() == MAX_ATTEMPTS) {
                    removeState(internalPort);
                    dps.getFuture().setException(new PortMapException("Unable to unmap"));
                } else {
                    dps.incrementAttempt();
                    MapPcpRequest mapPcpRequest = controller.requestMapOperationAsync(internalPort.getPortType(),
                            internalPort.getPortNumber(), 0, InetAddress.getByName("::"), 0);
                    dps.setRequest(mapPcpRequest);
                    
                    long hitTime = System.currentTimeMillis() + PcpUtils.getPcpWaitTime(dps.getAttempt() + 1);
                    PortEvent newEvent = new PortEvent(dps.getInternalPort(), hitTime);
                    dps.addEvent(newEvent);
                    portEvents.add(newEvent);
                }
            }
        }
    }

    @Override
    protected void shutDown() throws Exception {
        for (PortState state : portStates.values()) {
            SettableFuture<?> future;
            if (state instanceof CreatePortState) {
                future = ((CreatePortState) state).getFuture();
            } else if (state instanceof DestroyPortState) {
                future = ((DestroyPortState) state).getFuture();
            } else {
                continue;
            }

            if (!future.isDone()) {
                future.setException(new IllegalStateException("Closed"));
            }
        }

        for (Object msg : msgQueue) {
            if (msg instanceof MapPortMessage) {
                ((MapPortMessage) msg).getFuture().setException(new IllegalStateException("Closed"));
            } else if (msg instanceof UnmapPortMessage) {
                ((UnmapPortMessage) msg).getFuture().setException(new IllegalStateException("Closed"));
            }
        }
    }

    @Override
    protected void triggerShutdown() {
        msgQueue.add(new StopMessage());
    }

    @Override
    protected String serviceName() {
        return "PCP Port Mapper Thread";
    }

    @Override
    protected Executor executor() {
        return new Executor() {
            @Override
            public void execute(Runnable command) {
                Thread thread = new Thread(command);
                thread.setDaemon(true);
                thread.start();
            }
        };
    }

    private static final class ServicePcpControllerListener implements PcpControllerListener {

        private LinkedBlockingQueue<Object> msgQueue;

        public ServicePcpControllerListener(LinkedBlockingQueue<Object> msgQueue) {
            Validate.notNull(msgQueue);
            this.msgQueue = msgQueue;
        }

        @Override
        public void incomingResponse(CommunicationType type, PcpResponse response) {
            msgQueue.add(new PcpResponseMessage(response));
        }
    }

    private static final class PortEventComparator implements Comparator<PortEvent> {

        @Override
        public int compare(PortEvent o1, PortEvent o2) {
            return Long.compare(o1.getHitTime(), o2.getHitTime());
        }
    }

    private static final class PortEvent {

        private Port internalPort;
        private long hitTime;
        private boolean cancel;

        public PortEvent(Port internalPort, long hitTime) {
            Validate.notNull(internalPort);
            Validate.inclusiveBetween(0L, Long.MAX_VALUE, hitTime);
            this.internalPort = internalPort;
            this.hitTime = hitTime;
        }

        public Port getInternalPort() {
            return internalPort;
        }

        public long getHitTime() {
            return hitTime;
        }

        public boolean isCancelled() {
            return cancel;
        }

        public void cancel() {
            cancel = true;
        }
    }

    private abstract class PortState {

        private Set<PortEvent> events;
        private Port internalPort;

        public PortState(Port internalPort) {
            Validate.notNull(internalPort);
            this.internalPort = internalPort;
            events = new HashSet<>();
        }

        public void addEvent(PortEvent e) {
            events.add(e);
        }

        public void removeEvent(PortEvent e) {
            events.remove(e);
        }

        public Port getInternalPort() {
            return internalPort;
        }

        public Set<PortEvent> viewEvents() {
            return Collections.unmodifiableSet(events);
        }
    }

    private static final class CreatePortState extends PortState {

        private int attempt;
        private SettableFuture<MappedPort> future;
        private MapPcpRequest request;

        public CreatePortState(Port internalPort, MapPcpRequest request, SettableFuture<MappedPort> future) {
            super(internalPort);
            Validate.notNull(internalPort);
            Validate.notNull(request);
            Validate.notNull(future);
            this.future = future;
            this.request = request;
        }

        public MapPcpRequest getRequest() {
            return request;
        }

        public void setRequest(MapPcpRequest request) {
            this.request = request;
        }

        public int getAttempt() {
            return attempt;
        }

        public SettableFuture<MappedPort> getFuture() {
            return future;
        }

        public void incrementAttempt() {
            attempt++;
        }
    }

    private static final class MaintainPortState extends PortState {

        private int attempt;
        private long duration;
        private MapPcpRequest request;
        private int externalPort;
        private InetAddress externalAddress;

        public MaintainPortState(Port internalPort, MapPcpRequest request, int externalPort, InetAddress externalAddress) {
            super(internalPort);
            Validate.notNull(internalPort);
            Validate.notNull(request);
            Validate.inclusiveBetween(1, 65535, externalPort);
            Validate.notNull(externalAddress);
            this.externalPort = externalPort;
            this.externalAddress = externalAddress;
        }

        public MapPcpRequest getRequest() {
            return request;
        }

        public void setRequest(MapPcpRequest request) {
            this.request = request;
        }

        public int getExternalPort() {
            return externalPort;
        }

        public InetAddress getExternalAddress() {
            return externalAddress;
        }

        public int getAttempt() {
            return attempt;
        }

        public void incrementAttempt() {
            attempt++;
        }

        public void resetAttempt() {
            attempt = 0;
        }
    }

    private static final class DestroyPortState extends PortState {

        private int attempt;
        private SettableFuture<Void> future;
        private MapPcpRequest request;

        public DestroyPortState(Port internalPort, MapPcpRequest request, SettableFuture<Void> future) {
            super(internalPort);
            Validate.notNull(internalPort);
            Validate.notNull(request);
            Validate.notNull(future);
            this.future = future;
            this.request = request;
        }

        public MapPcpRequest getRequest() {
            return request;
        }

        public void setRequest(MapPcpRequest request) {
            this.request = request;
        }
        
        public int getAttempt() {
            return attempt;
        }

        public SettableFuture<Void> getFuture() {
            return future;
        }

        public void incrementAttempt() {
            attempt++;
        }
    }

    static final class MapPortMessage {

        private Port internalPort;
        private SettableFuture<MappedPort> future;

        public MapPortMessage(Port internalPort) {
            Validate.notNull(internalPort);
            this.internalPort = internalPort;
            this.future = SettableFuture.create();
        }

        public Port getInternalPort() {
            return internalPort;
        }

        public SettableFuture<MappedPort> getFuture() {
            return future;
        }
    }

    static final class UnmapPortMessage {

        private Port internalPort;
        private SettableFuture<Void> future;

        public UnmapPortMessage(Port internalPort) {
            Validate.notNull(internalPort);
            this.internalPort = internalPort;
            this.future = SettableFuture.create();
        }

        public Port getInternalPort() {
            return internalPort;
        }

        public SettableFuture<Void> getFuture() {
            return future;
        }
    }

    static final class PcpResponseMessage {

        private PcpResponse response;

        public PcpResponseMessage(PcpResponse response) {
            Validate.notNull(response);
            this.response = response;
        }

        public PcpResponse getResponse() {
            return response;
        }

    }

    static final class StopMessage {
    }
}
