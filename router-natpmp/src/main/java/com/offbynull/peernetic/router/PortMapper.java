package com.offbynull.peernetic.router;

import java.io.Closeable;
import org.apache.commons.lang3.Validate;

public abstract class PortMapper implements Closeable {
    private PortMapperEventListener portMapperListener;

    public PortMapper(PortMapperEventListener portMapperListener) {
        Validate.notNull(portMapperListener);
        this.portMapperListener = portMapperListener;
    }

    protected abstract void mapPort(PortType portType, int internalPort);
    
    protected abstract void unmapPort(PortType portType, int internalPort);
    
    protected final PortMapperEventListener getListener() {
        return portMapperListener;
    }
}
