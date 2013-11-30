package com.offbynull.rpccommon;

import com.offbynull.rpccommon.filters.accesscontrol.BlacklistIncomingFilter;
import com.offbynull.rpccommon.filters.accesscontrol.BlacklistIncomingFilter.AddressInBlacklistException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class BlacklistIncomingMessageListenerTest {
    
    public BlacklistIncomingMessageListenerTest() {
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
