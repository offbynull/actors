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
package com.offbynull.peernetic.rpc.common.services.nat;

/**
 * NAT helper service.
 * @author Kasra F
 */
public interface NatHelperService {
    /**
     * Service ID.
     */
    int SERVICE_ID = 2000;
    
    /**
     * Gets the address of the client that invoked the method.
     * @return address of the client that invoked the method
     */
    String getAddress();
    
    /**
     * Tests to see if a certain port is active on the address of the client that invoked the method. Method does not throw an exceptions,
     * but will return {@link TestPortResult#ERROR} if something an argument is incorrect or something went wrong.
     * @param type connection type
     * @param port port to test
     * @param challenge challenge bytes (must be 8 bytes long)
     * @return results of the port test
     */
    TestPortResult testPort(ConnectionType type, int port, byte[] challenge);
    
    /**
     * Connection type.
     */
    public enum ConnectionType {
        /** TCP connection type. */
        TCP,
        /** UDP connection type. */
        UDP
    }
    
    /**
     * Result type for {@link #testPort(com.offbynull.rpccommon.services.nat.NatHelperService.ConnectionType, int, byte[]) }.
     */
    public enum TestPortResult {
        /** Successful in testing port. */
        SUCCESS,
        /** Not successful in testing port. */
        FAIL,
        /** Error while trying to test port. */
        ERROR
    }
}
