/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.debug.testnetwork;

import com.offbynull.peernetic.debug.testnetwork.messages.ArriveMessage;
import com.offbynull.peernetic.debug.testnetwork.messages.DepartMessage;
import com.offbynull.peernetic.debug.testnetwork.messages.TransitMessage;
import java.util.Collection;

/**
 * Controls how a test network behaves. For example, depending on the line, a message may be dropped/duplicated/corrupted/slow/fast/etc...
 * @author Kasra Faghihi
 * @param <A> address type
 */
public interface Line<A> {
    /**
     * Called when a message is put on the network.
     * @param departMessage outgoing message
     * @return list of {@link TransitMessage} objects generated from {@code departMessage}
     */
    Collection<TransitMessage<A>> depart(DepartMessage<A> departMessage);
    
    /**
     * Called when a message on the network reaches its destination.
     * @param transitMessage message that has arrived
     * @return list of {@link ArriveMessage} objects generated from {@code transitMessage}
     */
    Collection<ArriveMessage<A>> arrive(TransitMessage<A> transitMessage);
}