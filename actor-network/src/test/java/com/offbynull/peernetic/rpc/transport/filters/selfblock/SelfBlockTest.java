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
package com.offbynull.peernetic.rpc.transport.filters.selfblock;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import junit.framework.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SelfBlockTest {
    
    public SelfBlockTest() {
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

    @Test(expected = RuntimeException.class)
    public void ensureBlockedOnSameIdTest() throws UnsupportedEncodingException {
        SelfBlockId id = new SelfBlockId();
        
        SelfBlockOutgoingFilter<Integer> outgoingFilter = new SelfBlockOutgoingFilter<>(id);
        SelfBlockIncomingFilter<Integer> incomingFilter = new SelfBlockIncomingFilter<>(id);
        
        ByteBuffer msg = outgoingFilter.filter(1, ByteBuffer.wrap("hello world".getBytes("UTF-8")));
        incomingFilter.filter(0, msg);
    }

    @Test
    public void ensurePassOnDifferentIdTest() throws UnsupportedEncodingException {
        SelfBlockId idOne = new SelfBlockId();
        SelfBlockId idTwo = new SelfBlockId();
        
        SelfBlockOutgoingFilter<Integer> outgoingFilter = new SelfBlockOutgoingFilter<>(idOne);
        SelfBlockIncomingFilter<Integer> incomingFilter = new SelfBlockIncomingFilter<>(idTwo);
        
        ByteBuffer outgoingMsg = ByteBuffer.wrap("hello world".getBytes("UTF-8"));
        ByteBuffer transitMsg = outgoingFilter.filter(1, outgoingMsg);
        ByteBuffer incomingMsg = incomingFilter.filter(0, transitMsg);
        
        outgoingMsg.flip();
        Assert.assertEquals(outgoingMsg, incomingMsg);
    }
}
