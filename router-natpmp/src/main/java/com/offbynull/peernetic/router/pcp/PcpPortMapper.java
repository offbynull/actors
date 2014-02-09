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
    private Map<ImmutablePair<PortType, Integer>, RefreshTask> openPorts; // used to refresh mappings periodically

    public PcpPortMapper(InetAddress gatewayAddress, InetAddress selfAddress, PortMapperEventListener listener) {
        super(listener);
        
        random = new Random();
        controller = new PcpController(random, gatewayAddress, selfAddress, new CustomPcpControllerListener());
        timer = new Timer("PCP Client Refresh Timer", true);
        openPorts = new HashMap<>();
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
                RefreshTask refreshTask = new RefreshTask(mappedPort);
                openPorts.put(new ImmutablePair<>(portType, mappedPort.getInternalPort()), refreshTask);
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
                TimerTask task = openPorts.remove(new ImmutablePair<>(portType, internalPort));
                task.cancel();
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
    
    private class RefreshTask extends TimerTask {

        private MappedPort portDetails;

        public RefreshTask(MappedPort portDetails) {
            Validate.notNull(portDetails);
            this.portDetails = portDetails;
        }
        
        @Override
        public void run() {
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
                    RefreshTask task = openPorts.get(new ImmutablePair<>(portType, mapPcpResponse.getInternalPort()));
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
            }
        }
    }
}
