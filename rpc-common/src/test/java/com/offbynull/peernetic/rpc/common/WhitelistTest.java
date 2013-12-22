package com.offbynull.peernetic.rpc.common;

import com.offbynull.peernetic.rpc.common.filters.accesscontrol.WhitelistIncomingFilter;
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
