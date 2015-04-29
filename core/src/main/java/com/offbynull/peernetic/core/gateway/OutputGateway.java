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
package com.offbynull.peernetic.core.gateway;

import com.offbynull.peernetic.core.shuttle.Shuttle;

// this is when the gateway can write dynamically to multiple sources, if you're only going to dump to a known number of shuttles that are
// finite, you could implement a basic Gateway and pass thsoe shuttles in through a constructor
public interface OutputGateway extends Gateway {
    void addOutgoingShuttle(Shuttle shuttle);
    void removeOutgoingShuttle(String shuttlePrefix);
}
