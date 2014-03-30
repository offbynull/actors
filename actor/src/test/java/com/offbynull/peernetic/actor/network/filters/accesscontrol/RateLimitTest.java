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

import com.offbynull.peernetic.actor.network.filters.accesscontrol.RateLimitIncomingFilter;
import com.offbynull.peernetic.actor.network.filters.accesscontrol.RateLimitIncomingFilter.AddressBannedException;
import java.nio.ByteBuffer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RateLimitTest {
    
    public RateLimitTest() {
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

     @Test(expected = AddressBannedException.class)
     public void banTest() {
         RateLimitIncomingFilter<Integer> filter = new RateLimitIncomingFilter<>(5, 10000L);
         
         filter.filter(5, ByteBuffer.allocate(0));
         filter.filter(5, ByteBuffer.allocate(0));
         filter.filter(5, ByteBuffer.allocate(0));
         filter.filter(5, ByteBuffer.allocate(0));
         filter.filter(5, ByteBuffer.allocate(0));
         filter.filter(5, ByteBuffer.allocate(0));
     }

     @Test
     public void resetTest() throws InterruptedException {
         RateLimitIncomingFilter<Integer> filter = new RateLimitIncomingFilter<>(3, 300L);
         
         filter.filter(5, ByteBuffer.allocate(0));
         filter.filter(5, ByteBuffer.allocate(0));
         filter.filter(5, ByteBuffer.allocate(0));
         Thread.sleep(350L); // +50L just to be safe
         filter.filter(5, ByteBuffer.allocate(0));
         filter.filter(5, ByteBuffer.allocate(0));
         filter.filter(5, ByteBuffer.allocate(0));
     }

     @Test
     public void clearBannedTest() throws InterruptedException {
         RateLimitIncomingFilter<Integer> filter = new RateLimitIncomingFilter<>(3, 10000L);
         
         filter.filter(5, ByteBuffer.allocate(0));
         filter.filter(5, ByteBuffer.allocate(0));
         filter.filter(5, ByteBuffer.allocate(0));
         boolean thrown = false;
         try {
             filter.filter(5, ByteBuffer.allocate(0));
         } catch (AddressBannedException e) {
             thrown = true;
         }
         Assert.assertTrue(thrown);
         
         filter.clearBanned();
         filter.filter(5, ByteBuffer.allocate(0));
         filter.filter(5, ByteBuffer.allocate(0));
         filter.filter(5, ByteBuffer.allocate(0));
     }
}
