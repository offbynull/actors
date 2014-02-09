package com.offbynull.peernetic.router;

public interface PortMapperEventListener {
    void needRestart(Object reason);
    void mappingChanged(MappedPort oldMappedPort, MappedPort newMappedPort);
}
