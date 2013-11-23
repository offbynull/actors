package com.offbynull.rpccommon;

import com.offbynull.rpc.transport.IncomingMessage;
import com.offbynull.rpc.transport.IncomingMessageResponseHandler;
import com.offbynull.rpccommon.filters.WhitelistIncomingMessageListener;
import java.nio.ByteBuffer;
import java.util.HashSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class WhitelistIncomingMessageListenerTest {
    
    public WhitelistIncomingMessageListenerTest() {
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
        
        WhitelistIncomingMessageListener<Integer> whitelistFilter = new WhitelistIncomingMessageListener<>(initialAllowList);
        IncomingMessageResponseHandler mockedResponseHandler = Mockito.mock(IncomingMessageResponseHandler.class);
        
        IncomingMessage<Integer> messageFrom5 = new IncomingMessage<>(5, ByteBuffer.allocate(0), 0L);
        
        whitelistFilter.messageArrived(messageFrom5, mockedResponseHandler);
    }
    
    @Test
    public void addedTest() {
        WhitelistIncomingMessageListener<Integer> whitelistFilter = new WhitelistIncomingMessageListener<>();
        IncomingMessageResponseHandler mockedResponseHandler = Mockito.mock(IncomingMessageResponseHandler.class);
        
        whitelistFilter.addAddress(5);
        
        IncomingMessage<Integer> messageFrom5 = new IncomingMessage<>(5, ByteBuffer.allocate(0), 0L);
        
        whitelistFilter.messageArrived(messageFrom5, mockedResponseHandler);
    }

    @Test(expected = WhitelistIncomingMessageListener.AddressNotInWhitelistException.class)
    public void addedThenRemovedTest() {
        WhitelistIncomingMessageListener<Integer> whitelistFilter = new WhitelistIncomingMessageListener<>();
        IncomingMessageResponseHandler mockedResponseHandler = Mockito.mock(IncomingMessageResponseHandler.class);
        
        whitelistFilter.addAddress(5);
        whitelistFilter.removeAddress(5);
        
        IncomingMessage<Integer> messageFrom5 = new IncomingMessage<>(5, ByteBuffer.allocate(0), 0L);
        
        whitelistFilter.messageArrived(messageFrom5, mockedResponseHandler);
    }

    @Test(expected = WhitelistIncomingMessageListener.AddressNotInWhitelistException.class)
    public void addedThenClearedTest() {
        WhitelistIncomingMessageListener<Integer> whitelistFilter = new WhitelistIncomingMessageListener<>();
        IncomingMessageResponseHandler mockedResponseHandler = Mockito.mock(IncomingMessageResponseHandler.class);
        
        whitelistFilter.addAddress(5);
        whitelistFilter.clear();
        
        IncomingMessage<Integer> messageFrom5 = new IncomingMessage<>(5, ByteBuffer.allocate(0), 0L);
        
        whitelistFilter.messageArrived(messageFrom5, mockedResponseHandler);
    }

    @Test(expected = WhitelistIncomingMessageListener.AddressNotInWhitelistException.class)
    public void neverAddedTest() {
        WhitelistIncomingMessageListener<Integer> whitelistFilter = new WhitelistIncomingMessageListener<>();
        IncomingMessageResponseHandler mockedResponseHandler = Mockito.mock(IncomingMessageResponseHandler.class);
        
        IncomingMessage<Integer> messageFrom5 = new IncomingMessage<>(5, ByteBuffer.allocate(0), 0L);
        
        whitelistFilter.messageArrived(messageFrom5, mockedResponseHandler);
    }
}
