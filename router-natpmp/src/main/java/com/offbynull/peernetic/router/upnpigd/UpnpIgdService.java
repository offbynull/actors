package com.offbynull.peernetic.router.upnpigd;

import java.util.Objects;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.Validate;

public class UpnpIgdService {

    private UpnpIgdServiceReference service;

    private Range<Long> leaseDurationRange;
    private Range<Long> externalPortRange;

    public UpnpIgdService(UpnpIgdServiceReference service, Range<Long> leaseDurationRange, Range<Long> externalPortRange) {
        Validate.notNull(service);

        this.service = service;
        this.leaseDurationRange = leaseDurationRange;
        this.externalPortRange = externalPortRange;
    }

    public UpnpIgdServiceReference getService() {
        return service;
    }

    public Range<Long> getLeaseDurationRange() {
        return leaseDurationRange;
    }

    public Range<Long> getExternalPortRange() {
        return externalPortRange;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(this.service);
        hash = 79 * hash + Objects.hashCode(this.leaseDurationRange);
        hash = 79 * hash + Objects.hashCode(this.externalPortRange);
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
        if (!Objects.equals(this.service, other.service)) {
            return false;
        }
        if (!Objects.equals(this.leaseDurationRange, other.leaseDurationRange)) {
            return false;
        }
        if (!Objects.equals(this.externalPortRange, other.externalPortRange)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "UpnpIgdService{" + "service=" + service + ", leaseDurationRange=" + leaseDurationRange
                + ", externalPortRange=" + externalPortRange + '}';
    }

}
