/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
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
package com.offbynull.actors.redisclient;

/**
 * Block that defines watch conditions.
 * @author Kasra Faghihi
 */
public interface WatchBlock {
    /**
     * Performs Redis operations to check that certain conditions are met.
     * @return if conditions were met
     * @throws ConnectionException if there was a problem with redis or the connection to redis
     * @throws IllegalStateException if connector closed
     */
    boolean execute() throws ConnectionException; 
}
