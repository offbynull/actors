/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.actor.network.transports.test;

import java.io.Serializable;
import java.util.Comparator;

final class TransitMessageArriveTimeComparator implements Comparator<TransitMessage>, Serializable {
    
    private static final long serialVersionUID = 0L;

    @Override
    public int compare(TransitMessage o1, TransitMessage o2) {
        return Long.compare(o1.getArriveTime(), o2.getArriveTime());
    }
    
}
