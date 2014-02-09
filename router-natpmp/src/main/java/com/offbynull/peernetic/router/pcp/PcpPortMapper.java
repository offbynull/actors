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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;

public final class PcpPortMapper extends PortMapper {
    private Random random;
    private PcpController controller;
    private Timer timer;
    private Lock lock;
    private Map<ImmutablePair<PortType, Integer>, RefreshTask> refreshTasks; // used to refresh mappings periodically
    private Map<ImmutablePair<PortType, Integer>, NotifyDeathTask> deathTasks; // used to notify when mappings haven't been refreshed

    public PcpPortMapper(InetAddress gatewayAddress, InetAddress selfAddress, PortMapperEventListener listener) {
        super(listener);
        
        random = new Random();
        controller = new PcpController(random, gatewayAddress, selfAddress, new CustomPcpControllerListener());
        timer = new Timer("PCP Client Timer", true);
        refreshTasks = new HashMap<>();
        deathTasks = new HashMap<>();
        lock = new ReentrantLock();
    }
    
    @Override
    public MappedPort mapPort(PortType portType) throws InterruptedException {
        Validate.notNull(portType);
        
        InetAddress suggestedExternalAddr;
        try {
            suggestedExternalAddr = InetAddress.getByName("::");
        } catch (UnknownHostException uhe) { // should never happen
            throw new IllegalStateException(uhe);
        }
        
        try {
            MapPcpResponse response = controller.requestMapOperation(4, portType, 0, 0, suggestedExternalAddr, 3600);
            MappedPort mappedPort = new MappedPort(response.getInternalPort(), response.getAssignedExternalPort(),
                    response.getAssignedExternalIpAddress(), portType);
            
            long actualLifetime = response.getLifetime();
            
            if (actualLifetime == 0L) {
                throw new PortMapException("Server returned 0 lifetime");
            }
            
            lock.lock();
            try {
                ImmutablePair<PortType, Integer> key = new ImmutablePair<>(portType, mappedPort.getInternalPort());
                
                NotifyDeathTask notifyDeathTask = new NotifyDeathTask(mappedPort);
                deathTasks.put(key, notifyDeathTask);
                timer.schedule(notifyDeathTask, actualLifetime);
                
                RefreshTask refreshTask = new RefreshTask(mappedPort);
                refreshTasks.put(key, refreshTask);
                timer.schedule(refreshTask, random.nextInt((int) (actualLifetime / 4L)));
            } finally {
                lock.unlock();
            }
            
            return mappedPort;
        } catch (PcpNoResponseException | IllegalArgumentException | BufferUnderflowException e) {
            throw new PortMapException(e);
        }
    }
    
    @Override
    public void unmapPort(PortType portType, int internalPort) throws InterruptedException {
        Validate.notNull(portType);
        
        InetAddress suggestedExternalAddr;
        try {
            suggestedExternalAddr = InetAddress.getByName("::");
        } catch (UnknownHostException uhe) { // should never happen
            throw new IllegalStateException(uhe);
        }
        
        try {
            controller.requestMapOperation(4, portType, 0, 0, suggestedExternalAddr, 0L);

            lock.lock();
            try {
                ImmutablePair<PortType, Integer> key = new ImmutablePair<>(portType, internalPort);
                refreshTasks.remove(key).cancel();
                deathTasks.remove(key).cancel();
            } finally {
                lock.unlock();
            }
        } catch (PcpNoResponseException | IllegalArgumentException | BufferUnderflowException e) { // NOPMD
            // do nothing
        }
    }

    @Override
    public void close() throws IOException {
        timer.cancel();
        timer.purge();
        controller.close();
    }
    
    private class NotifyDeathTask extends TimerTask {
        
        private MappedPort portDetails;

        public NotifyDeathTask(MappedPort portDetails) {
            Validate.notNull(portDetails);
            this.portDetails = portDetails;
        }

        @Override
        public void run() {
            lock.lock();
            try {
                ImmutablePair<PortType, Integer> key = new ImmutablePair<>(portDetails.getPortType(), portDetails.getInternalPort());
                refreshTasks.remove(key).cancel();
                deathTasks.remove(key).cancel();
                
                timer.cancel();
                timer.purge();
            } finally {
                lock.unlock();
            }

            getListener().needRestart("Unable to refresh portmapping: " + portDetails);
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
                    3600L);
        }

        public MappedPort getPortDetails() {
            return portDetails;
        }
        
    }
    
    private class CustomPcpControllerListener implements PcpControllerListener {

        @Override
        public void incomingResponse(CommunicationType type, PcpResponse response) {
            if (response instanceof AnnouncePcpResponse) {
                
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
                
                MappedPort newPortDetails = new MappedPort(mapPcpResponse.getInternalPort(), mapPcpResponse.getAssignedExternalPort(),
                        mapPcpResponse.getAssignedExternalIpAddress(), portType);
                
                
                // get old port details object
                MappedPort oldPortDetails;
                lock.lock();
                try {
                    RefreshTask task = refreshTasks.get(new ImmutablePair<>(portType, mapPcpResponse.getInternalPort()));
                    
                    if (task == null) {
                        return;
                    }
                    
                    oldPortDetails = task.getPortDetails();
                } finally {
                    lock.unlock();
                }
                
                
                // compare and notify if different
                if (!oldPortDetails.equals(newPortDetails)) {
                    try {
                        getListener().mappingChanged(oldPortDetails, newPortDetails);
                    } catch (RuntimeException re) { // NOPMD
                        // do nothing
                    }
                }
                
                // reset tasks
                int newLifetime = (int) Math.max(Integer.MAX_VALUE, mapPcpResponse.getLifetime());
                
                lock.lock();
                try {
                    ImmutablePair<PortType, Integer> oldKey = new ImmutablePair<>(oldPortDetails.getPortType(),
                            oldPortDetails.getInternalPort());
                    deathTasks.remove(oldKey).cancel();
                    refreshTasks.remove(oldKey).cancel();

                    
                    ImmutablePair<PortType, Integer> newKey = new ImmutablePair<>(newPortDetails.getPortType(),
                            newPortDetails.getInternalPort());

                    NotifyDeathTask notifyDeathTask = new NotifyDeathTask(newPortDetails);
                    deathTasks.put(newKey, notifyDeathTask);
                    timer.schedule(notifyDeathTask, newLifetime);

                    RefreshTask refreshTask = new RefreshTask(newPortDetails);
                    refreshTasks.put(newKey, refreshTask);
                    timer.schedule(refreshTask, random.nextInt((int) (newLifetime / 4L)));
                } finally {
                    lock.unlock();
                }
            }
        }
    }
}
