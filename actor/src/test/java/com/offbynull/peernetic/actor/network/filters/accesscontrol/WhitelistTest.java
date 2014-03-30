/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.actor.network.filters.accesscontrol;

import com.offbynull.peernetic.actor.network.filters.accesscontrol.WhitelistIncomingFilter;
import java.nio.ByteBuffer;
import java.util.HashSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class WhitelistTest {
    
    public WhitelistTest() {
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

    @Test
    public void constructorTest() {
        HashSet<Integer> initialAllowList = new HashSet<>();
        initialAllowList.add(5);
        
        WhitelistIncomingFilter<Integer> whitelistFilter = new WhitelistIncomingFilter<>(initialAllowList);
        
        whitelistFilter.filter(5, ByteBuffer.allocate(0));
    }
    
    @Test
    public void addedTest() {
        WhitelistIncomingFilter<Integer> whitelistFilter = new WhitelistIncomingFilter<>();
        
        whitelistFilter.addAddress(5);
        
        whitelistFilter.filter(5, ByteBuffer.allocate(0));
    }

    @Test(expected = WhitelistIncomingFilter.AddressNotInWhitelistException.class)
    public void addedThenRemovedTest() {
        WhitelistIncomingFilter<Integer> whitelistFilter = new WhitelistIncomingFilter<>();
        
        whitelistFilter.addAddress(5);
        whitelistFilter.removeAddress(5);
        
        whitelistFilter.filter(5, ByteBuffer.allocate(0));
    }

    @Test(expected = WhitelistIncomingFilter.AddressNotInWhitelistException.class)
    public void addedThenClearedTest() {
        WhitelistIncomingFilter<Integer> whitelistFilter = new WhitelistIncomingFilter<>();
        
        whitelistFilter.addAddress(5);
        whitelistFilter.clear();
        
        whitelistFilter.filter(5, ByteBuffer.allocate(0));
    }

    @Test(expected = WhitelistIncomingFilter.AddressNotInWhitelistException.class)
    public void neverAddedTest() {
        WhitelistIncomingFilter<Integer> whitelistFilter = new WhitelistIncomingFilter<>();
        
        whitelistFilter.filter(5, ByteBuffer.allocate(0));
    }
}
