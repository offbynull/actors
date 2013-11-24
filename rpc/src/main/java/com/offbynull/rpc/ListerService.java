package com.offbynull.rpc;

import java.util.Collections;
import java.util.List;

/**
 * A special service that provides callers with a list of supported services.
 * @author Kasra F
 */
public interface ListerService {

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
    public static final class Services {
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
