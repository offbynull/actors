package com.offbynull.rpc;

import com.offbynull.rpc.ListerService.Services;
import com.offbynull.rpc.invoke.AsyncResultListener;

/**
 * A special service that provides callers with a list of supported services.
 * @author Kasra F
 */
public interface ListerServiceAsync {

    /**
     * List service entries from {@code from} to {@code to}. For example, if you want to see the first 5 service entries, call with from set
     * to {@code 0} and to set to {@code 5}.
     * @param from start entry number (automatically tapered between {@code 0} and service count, so no out of bounds errors)
     * @param to stop entry number (automatically tapered between {@code stop} and service count, so no out of bounds errors)
     * @param result receives list of services
     * @throws NullPointerException if any arguments are {@code null}
     */
    void listServices(AsyncResultListener<Services> result, int from, int to);
    
    /**
     * Gets the name for a service.
     * @param id service id
     * @param result receives service name, or {@code null} if no such service exists
     * @throws NullPointerException if any arguments are {@code null}
     */
    void getServiceName(AsyncResultListener<String> result, int id);
}
