package com.offbynull.peernetic.router.upnpigd;

import com.offbynull.peernetic.router.upnpigd.UpnpIgdController.PortMappingInfo;

public interface UpnpIgdControllerListener {
    void mappingExpired(PortMappingInfo mappedPort);
}
