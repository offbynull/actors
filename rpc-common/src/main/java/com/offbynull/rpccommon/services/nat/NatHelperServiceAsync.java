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
package com.offbynull.rpccommon.services.nat;

import com.offbynull.rpc.invoke.AsyncResultListener;
import com.offbynull.rpccommon.services.nat.NatHelperService.ConnectionType;
import com.offbynull.rpccommon.services.nat.NatHelperService.TestPortResult;

/**
 * Async {@link NatHelperService}.
 * @author Kasra F
 */
public interface NatHelperServiceAsync {
    /**
     * See {@link NatHelperService#getAddress() }.
     * @param result result
     */
    void getAddress(AsyncResultListener<String> result);
    /**
     * See {@link NatHelperService#testPort(com.offbynull.rpccommon.services.nat.NatHelperService.ConnectionType, int, byte[]) }.
     * @param result result
     * @param type see {@link NatHelperService}
     * @param port see {@link NatHelperService}
     * @param challenge see {@link NatHelperService}
     */
    void testPort(AsyncResultListener<TestPortResult> result, ConnectionType type, int port, byte[] challenge);
}
