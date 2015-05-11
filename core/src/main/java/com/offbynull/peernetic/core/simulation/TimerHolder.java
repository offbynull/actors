/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.core.simulation;

import org.apache.commons.lang3.Validate;

final class TimerHolder implements Holder {
    private final String address;
    private final long granularity;

    public TimerHolder(String address, long granularity) {
        Validate.notNull(address);
        Validate.notNull(granularity);
        Validate.isTrue(granularity >= 0L);
        this.address = address;
        this.granularity = granularity;
    }

    public String getAddress() {
        return address;
    }

    public long getGranularity() {
        return granularity;
    }

}
