package com.offbynull.peernetic.router;

import java.io.Closeable;
import org.apache.commons.lang3.Validate;

public abstract class PortMapper implements Closeable {
    private PortMapperEventListener portMapperListener;

    public PortMapper(PortMapperEventListener portMapperListener) {
        Validate.notNull(portMapperListener);
        this.portMapperListener = portMapperListener;
    }

    protected abstract MappedPort mapPort(PortType portType) throws InterruptedException;
    
    protected abstract void unmapPort(PortType portType, int internalPort) throws InterruptedException;
    
    protected final PortMapperEventListener getListener() {
        return portMapperListener;
    }
}
