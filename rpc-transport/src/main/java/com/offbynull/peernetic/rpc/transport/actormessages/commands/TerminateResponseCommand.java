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
package com.offbynull.peernetic.rpc.transport.actormessages.commands;

/**
 * Terminate response.
 * @author Kasra Faghihi
 */
public final class TerminateResponseCommand {
    private long id;

    /**
     * Constructs a {@link OutgoingResponse} object.
     * @param id id
     * @throws NullPointerException if any arguments are {@code null}
     */
    public TerminateResponseCommand(long id) {
        this.id = id;
    }
    
    /**
     * Get Id.
     * @return id 
     */
    public long getId() {
        return id;
    }
}
