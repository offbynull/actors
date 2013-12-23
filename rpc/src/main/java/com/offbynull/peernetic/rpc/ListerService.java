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
package com.offbynull.peernetic.rpc;

import java.util.Collections;
import java.util.List;

/**
 * A special service that provides callers with a list of supported services.
 * @author Kasra Faghihi
 */
public interface ListerService {
    /**
     * Service ID.
     */
    int SERVICE_ID = 0; //public static final

    /**
     * List service entries from {@code from} to {@code to}. For example, if you want to see the first 5 service entries, call with from set
     * to {@code 0} and to set to {@code 5}.
     * @param from start entry number (automatically tapered between {@code 0} and service count, so no out of bounds errors)
     * @param to stop entry number (automatically tapered between {@code stop} and service count, so no out of bounds errors)
     * @return list of services
     */
    Services listServices(int from, int to);
    
    /**
     * Gets the name for a service.
     * @param id service id
     * @return service name, or {@code null} if no such service exists
     */
    String getServiceName(int id);
    
    
    /**
     * {@link ListerService#listServices(int, int) } result.
     */
    final class Services {
        private int total;
        private List<Integer> list;

        Services(int total, List<Integer> list) {
            this.total = total;
            this.list = Collections.unmodifiableList(list);
        }

        /**
         * Gets the total number of services available.
         * @return total number of services
         */
        public int getTotal() {
            return total;
        }

        /**
         * Gets the ids of the range requested.
         * @return ids of the range requested
         */
        public List<Integer> getList() {
            return Collections.unmodifiableList(list);
        }
        
    }
}
