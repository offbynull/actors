package com.offbynull.peernetic.router;

public interface PortMapperEventListener {
    void mappingCreationSuccessful(MappedPort oldMappedPort);
    void mappingCreationFailed(PortType portType, int internalPort);
    
    void mappingChanged(MappedPort oldMappedPort, MappedPort newMappedPort);
    
    void mappingLost(MappedPort oldMappedPort);
}
