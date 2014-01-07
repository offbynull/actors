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
package com.offbynull.peernetic.rpc.transport.filters.accesscontrol;

import com.offbynull.peernetic.rpc.transport.filters.accesscontrol.BlacklistIncomingFilter.AddressInBlacklistException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class BlacklistTest {
    
    public BlacklistTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test(expected = AddressInBlacklistException.class)
    public void constructorTest() {
        HashSet<Integer> initialBanList = new HashSet<>();
        initialBanList.add(5);
        
        BlacklistIncomingFilter<Integer> blacklistFilter = new BlacklistIncomingFilter<>(initialBanList);
        
        blacklistFilter.filter(5, ByteBuffer.allocate(0));
    }
    
    @Test(expected = AddressInBlacklistException.class)
    public void addedTest() {
        BlacklistIncomingFilter<Integer> blacklistFilter = new BlacklistIncomingFilter<>();
        
        blacklistFilter.addAddress(5);
        
        blacklistFilter.filter(5, ByteBuffer.allocate(0));
    }

    @Test
    public void addedThenRemovedTest() {
        BlacklistIncomingFilter<Integer> blacklistFilter = new BlacklistIncomingFilter<>();
        
        blacklistFilter.addAddress(5);
        blacklistFilter.removeAddress(5);
        
        blacklistFilter.filter(5, ByteBuffer.allocate(0));
    }

    @Test
    public void addedThenClearedTest() {
        BlacklistIncomingFilter<Integer> blacklistFilter = new BlacklistIncomingFilter<>();
        
        blacklistFilter.addAddress(5);
        blacklistFilter.clear();
        
        blacklistFilter.filter(5, ByteBuffer.allocate(0));
    }

    @Test
    public void neverAddedTest() {
        BlacklistIncomingFilter<Integer> blacklistFilter = new BlacklistIncomingFilter<>();
        
        blacklistFilter.filter(5, ByteBuffer.allocate(0));
    }
}
