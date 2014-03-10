package com.offbynull.peernetic.router.upnpigd;

import com.offbynull.peernetic.router.MappedPort;
import com.offbynull.peernetic.router.PortMapper;
import com.offbynull.peernetic.router.PortMapperEventListener;
import com.offbynull.peernetic.router.PortType;
import com.offbynull.peernetic.router.common.ResponseException;
import com.offbynull.peernetic.router.upnpigd.UpnpIgdController.PortMappingInfo;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;
import org.apache.commons.lang3.Validate;

public final class UpnpIgdPortMapper implements PortMapper {

    private Random random = new Random();
    private UpnpIgdController controller;
    private volatile boolean closed;

    public UpnpIgdPortMapper(UpnpIgdService service, InetAddress selfAddress, final PortMapperEventListener listener) {
        Validate.notNull(service);
        Validate.notNull(selfAddress);
        Validate.notNull(listener);

        controller = new UpnpIgdController(selfAddress, service, new UpnpIgdControllerListener() {
            @Override
            public void mappingExpired(UpnpIgdController.PortMappingInfo mappedPort) {
                if (closed) {
                    return;
                }

                listener.resetRequired("Mapping may have been lost: " + mappedPort);
            }
        });
    }

    @Override
    public MappedPort mapPort(PortType portType, int internalPort, long lifetime) throws InterruptedException {
        Validate.validState(!closed);
        Validate.notNull(portType);
        Validate.inclusiveBetween(1, 65535, internalPort);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, lifetime);
        
        int externalPort = random.nextInt(55535) + 10000; // 10000 - 65535
        PortMappingInfo info;
        InetAddress externalAddress;
        try {
            info = controller.addPortMapping(externalPort, internalPort, portType, lifetime);
            externalAddress = controller.getExternalIp();
        } catch (IllegalArgumentException | ResponseException re) {
            throw new IllegalStateException(re);
        }
        
        return new MappedPort(info.getInternalPort(), info.getExternalPort(), externalAddress, info.getPortType(),
                info.getRemainingDuration());
    }

    @Override
    public void unmapPort(MappedPort mappedPort) throws InterruptedException {
        Validate.validState(!closed);
        Validate.notNull(mappedPort);
        
        try {
            controller.deletePortMapping(mappedPort.getExternalPort(), mappedPort.getPortType());
        } catch (IllegalArgumentException | ResponseException re) {
            throw new IllegalStateException(re);
        }
    }

    @Override
    public MappedPort refreshPort(MappedPort mappedPort, long lifetime) throws InterruptedException {
        Validate.validState(!closed);
        Validate.notNull(mappedPort);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, lifetime);
        
        PortMappingInfo info;
        InetAddress externalAddress;
        try {
            controller.deletePortMapping(mappedPort.getExternalPort(), mappedPort.getPortType());
            info = controller.addPortMapping(mappedPort.getExternalPort(), mappedPort.getInternalPort(), mappedPort.getPortType(),
                    lifetime);
            externalAddress = controller.getExternalIp();
        } catch (IllegalArgumentException | ResponseException re) {
            throw new IllegalStateException(re);
        }
        
        return new MappedPort(info.getInternalPort(), info.getExternalPort(), externalAddress, info.getPortType(),
                info.getRemainingDuration());
    }

    @Override
    public void close() throws IOException {
        closed = true;
        controller.close();
    }

}
