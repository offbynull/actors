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
package com.offbynull.peernetic.router.natpmp;

import com.offbynull.peernetic.common.utils.ProcessUtils;
import com.offbynull.peernetic.common.utils.RegexUtils;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Utilities for NAT-PMP.
 * @author Kasra Faghihi
 */
public final class NatPmpUtils {
    /**
     * IPs grabbed from http://www.techspot.com/guides/287-default-router-ip-addresses/ + comments @ 1/19/2014.
     */
    @SuppressWarnings("PMD")
    private static final Set<String> PRESET_IPS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
        "192.168.1.1", // 3Com
        "10.0.1.1", // Apple
        "192.168.1.1", "192.168.1.220", // Asus
        "192.168.2.1", "10.1.1.1", // Belkin
        "192.168.15.1", // Cisco
        "192.168.11.1", // Buffalo
        "192.168.1.1", // Dell
        "192.168.0.1", "192.168.0.30", "192.168.0.50", "192.168.1.1", "192.10.1.1.1", // D-Link
        "192.168.0.1", "192.168.1.1", // Linksys
        "192.168.2.1", // Microsoft
        "192.168.10.1", "192.168.20.1", "192.168.30.1", "192.168.62.1", "192.168.100.1", "192.168.102.1", "192.168.1.254", // Motorola
        "192.168.1.254", // MSI
        "192.168.0.1", "192.168.0.227", "192.168.1.1", // Netgear
        "192.168.0.1", // Senao
        "10.0.0.138", "192.168.1.254", // SpeedTouch
        "10.0.0.138", // Telstra
        "192.168.1.1", // TP-LINK
        "192.168.0.1", "192.168.1.1", "192.168.2.1", "192.168.10.1", // Trendnet
        "192.168.1.1", "192.168.2.1", "192.168.123.254", // U.S. Robotics
        "192.168.1.1", "192.168.2.1", "192.168.4.1", "192.168.10.1", "192.168.1.254", "10.0.0.2", "10.0.0.138" // Zyxel
    )));
    
    private NatPmpUtils() {
        // do nothing
    }

    /**
     * Attempts to find one or more gateways that support NAT-PMP.
     * <p/>
     * This method first tries to find a gateway address by executing "netstat -rn" and scraping IPv4 addresses from the output. The command
     * should be available on Linux, OSX, and Windows. It also attempts to query a set of default router gateway address.
     * @return a list of possible gateway devices that support NAT-PMP
     * @throws InterruptedException if interrupted
     */
    public static List<InetAddress> findGateway() throws InterruptedException {
        // Ask OS for gateway address
        String netstatOutput = "";
        try {
            netstatOutput = ProcessUtils.runProcessAndDumpOutput(5000L, "netstat", "-rn");
        } catch (IOException ioe) { // NOPMD
            // do nothing
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
        }
        LinkedHashSet<String> addresses = new LinkedHashSet<>(RegexUtils.findAllIpv4Addresses(netstatOutput));
        
        
        // Push in defaults
        addresses.addAll(PRESET_IPS);
        
        
        // Send requests
        ThreadPoolExecutor executor = null;
        
        try {
            // Num of threads based on 'Determining the Number of Threads' section of chapter 1 of 'Programming Concurrency on the JVM'
            // Blocking coefficient estimated at 0.9
            double availableCores = Runtime.getRuntime().availableProcessors();
            int maxThreadCount = (int) (availableCores / (1.0 - 0.9));
            
            // If number of attempts you need to make is < number of threads allotted, reduce number of threads allotted
            int threadCount = Math.min(maxThreadCount, addresses.size());
            
            // Create executor and add in tasks
            executor = new ThreadPoolExecutor(threadCount, threadCount, 5L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
            
            List<Callable<InetAddress>> tasks = new LinkedList<>();
            for (final String address : addresses) {
                Callable<InetAddress> callable = new Callable<InetAddress>() {

                    @Override
                    public InetAddress call() throws Exception {
                        InetAddress inetAddress = InetAddress.getByName(address);
                        NatPmpController controller = new NatPmpController(inetAddress, 4);

                        controller.getExternalAddress(); // test
                        
                        return inetAddress;
                    }
                };
                
                tasks.add(callable);
            }
            
            List<Future<InetAddress>> futures = executor.invokeAll(tasks);
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            
            List<InetAddress> completed = new LinkedList<>();
            for (Future<InetAddress> future : futures) {
                InetAddress address;
                try {
                    address = future.get();
                } catch (ExecutionException ee) {
                    continue;
                }
                
                completed.add(address);
            }
            
            return completed;
        } finally {
            if (executor != null) {
                executor.shutdownNow();
            }
        }
            
    }
}
