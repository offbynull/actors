package com.offbynull.peernetic.router.upnpigd;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

public final class UpnpIgdService {
    private final UpnpIgdDevice device;
    private final String serviceType;
    private final String serviceId;
    private final URI controlUrl;
    private final URI eventSubUrl;
    private final URI scpdUrl;

    UpnpIgdService(UpnpIgdDevice device, String serviceType, String serviceId, String controlUrl, String eventSubUrl,
            String scpdUrl) throws MalformedURLException {
        Validate.notNull(device);
        Validate.notNull(serviceType);
        Validate.notNull(serviceId);
//        Validate.notNull(controlUrl);
//        Validate.notNull(eventSubUrl);
//        Validate.notNull(scpdUrl);
        this.device = device;
        this.serviceType = serviceType;
        this.serviceId = serviceId;
        
        URI baseUri = device.getUrl();
        this.controlUrl = controlUrl == null ? null : baseUri.resolve(controlUrl);
        this.eventSubUrl = eventSubUrl == null ? null : baseUri.resolve(eventSubUrl);
        this.scpdUrl = scpdUrl == null ? null : baseUri.resolve(scpdUrl);
    }

    public UpnpIgdDevice getDevice() {
        return device;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getServiceId() {
        return serviceId;
    }

    public URI getControlUrl() {
        return controlUrl;
    }

    public URI getEventSubUrl() {
        return eventSubUrl;
    }

    public URI getScpdUrl() {
        return scpdUrl;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Objects.hashCode(this.device);
        hash = 19 * hash + Objects.hashCode(this.serviceType);
        hash = 19 * hash + Objects.hashCode(this.serviceId);
        hash = 19 * hash + Objects.hashCode(this.controlUrl);
        hash = 19 * hash + Objects.hashCode(this.eventSubUrl);
        hash = 19 * hash + Objects.hashCode(this.scpdUrl);
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
        final UpnpIgdService other = (UpnpIgdService) obj;
        if (!Objects.equals(this.device, other.device)) {
            return false;
        }
        if (!Objects.equals(this.serviceType, other.serviceType)) {
            return false;
        }
        if (!Objects.equals(this.serviceId, other.serviceId)) {
            return false;
        }
        if (!Objects.equals(this.controlUrl, other.controlUrl)) {
            return false;
        }
        if (!Objects.equals(this.eventSubUrl, other.eventSubUrl)) {
            return false;
        }
        if (!Objects.equals(this.scpdUrl, other.scpdUrl)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "UpnpIgdService{" + "device=" + device + ", serviceType=" + serviceType + ", serviceId=" + serviceId + ", controlUrl="
                + controlUrl + ", eventSubUrl=" + eventSubUrl + ", scpdUrl=" + scpdUrl + '}';
    }
}
