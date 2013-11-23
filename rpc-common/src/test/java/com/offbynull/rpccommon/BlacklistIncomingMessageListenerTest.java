package com.offbynull.rpccommon;

import com.offbynull.rpc.transport.IncomingMessage;
import com.offbynull.rpc.transport.IncomingMessageResponseHandler;
import com.offbynull.rpccommon.filters.BlacklistIncomingMessageListener;
import com.offbynull.rpccommon.filters.BlacklistIncomingMessageListener.AddressInBlacklistException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

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
    public void constructorBlacklistTest() {
        HashSet<Integer> initialBanList = new HashSet<>();
        initialBanList.add(5);
        
        BlacklistIncomingMessageListener<Integer> blacklistFilter = new BlacklistIncomingMessageListener<>(initialBanList);
        IncomingMessageResponseHandler mockedResponseHandler = Mockito.mock(IncomingMessageResponseHandler.class);
        
        IncomingMessage<Integer> messageFrom5 = new IncomingMessage<>(5, ByteBuffer.allocate(0), 0L);
        
        blacklistFilter.messageArrived(messageFrom5, mockedResponseHandler);
    }
    
    @Test(expected = AddressInBlacklistException.class)
    public void addedBlacklistTest() {
        BlacklistIncomingMessageListener<Integer> blacklistFilter = new BlacklistIncomingMessageListener<>();
        IncomingMessageResponseHandler mockedResponseHandler = Mockito.mock(IncomingMessageResponseHandler.class);
        
        blacklistFilter.addAddress(5);
        
        IncomingMessage<Integer> messageFrom5 = new IncomingMessage<>(5, ByteBuffer.allocate(0), 0L);
        
        blacklistFilter.messageArrived(messageFrom5, mockedResponseHandler);
    }

    @Test
    public void addedThenRemovedBlacklistTest() {
        BlacklistIncomingMessageListener<Integer> blacklistFilter = new BlacklistIncomingMessageListener<>();
        IncomingMessageResponseHandler mockedResponseHandler = Mockito.mock(IncomingMessageResponseHandler.class);
        
        blacklistFilter.addAddress(5);
        blacklistFilter.removeAddress(5);
        
        IncomingMessage<Integer> messageFrom5 = new IncomingMessage<>(5, ByteBuffer.allocate(0), 0L);
        
        blacklistFilter.messageArrived(messageFrom5, mockedResponseHandler);
    }

    @Test
    public void neverAddedBlacklistTest() {
        BlacklistIncomingMessageListener<Integer> blacklistFilter = new BlacklistIncomingMessageListener<>();
        IncomingMessageResponseHandler mockedResponseHandler = Mockito.mock(IncomingMessageResponseHandler.class);
        
        IncomingMessage<Integer> messageFrom5 = new IncomingMessage<>(5, ByteBuffer.allocate(0), 0L);
        
        blacklistFilter.messageArrived(messageFrom5, mockedResponseHandler);
    }
}
