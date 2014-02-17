package com.offbynull.peernetic.router.upnpigd;

import java.net.InetAddress;
import java.net.URL;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

public final class UpnpIgdDevice {
    private InetAddress gatewayAddress;
    private String name;
    private URL url;

    public UpnpIgdDevice(InetAddress gatewayAddress, String name, URL url) {
        Validate.notNull(gatewayAddress);
        Validate.notNull(url);
        this.gatewayAddress = gatewayAddress;
        this.name = name;
        this.url = url;
    }

    public InetAddress getGatewayAddress() {
        return gatewayAddress;
    }

    public String getName() {
        return name;
    }

    public URL getUrl() {
        return url;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.gatewayAddress);
//        hash = 17 * hash + Objects.hashCode(this.name);
//        hash = 17 * hash + Objects.hashCode(this.url);
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
        final UpnpIgdDevice other = (UpnpIgdDevice) obj;
        if (!Objects.equals(this.gatewayAddress, other.gatewayAddress)) {
            return false;
        }
//        if (!Objects.equals(this.name, other.name)) {
//            return false;
//        }
//        if (!Objects.equals(this.url, other.url)) {
//            return false;
//        }
        return true;
    }

    @Override
    public String toString() {
        return "Device{" + "gatewayAddress=" + gatewayAddress + ", name=" + name + ", url=" + url + '}';
    }

}
