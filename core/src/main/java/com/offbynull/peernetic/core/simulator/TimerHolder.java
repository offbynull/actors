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
package com.offbynull.peernetic.core.simulator;

import com.offbynull.peernetic.core.shuttle.Address;
import org.apache.commons.lang3.Validate;

final class TimerHolder implements Holder {
    private final Address address;
    
    public TimerHolder(Address address) {
        Validate.notNull(address);
        Validate.isTrue(!address.isEmpty());
        this.address = address;
    }

    @Override
    public Address getAddress() {
        return address;
    }

}
