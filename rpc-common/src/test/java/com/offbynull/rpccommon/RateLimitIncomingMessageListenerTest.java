package com.offbynull.rpccommon;

import com.offbynull.rpc.transport.IncomingMessage;
import com.offbynull.rpc.transport.IncomingMessageResponseHandler;
import com.offbynull.rpccommon.filters.RateLimitIncomingMessageListener;
import com.offbynull.rpccommon.filters.RateLimitIncomingMessageListener.AddressBannedException;
import java.nio.ByteBuffer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class RateLimitIncomingMessageListenerTest {
    
    public RateLimitIncomingMessageListenerTest() {
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
         RateLimitIncomingMessageListener<Integer> filter = new RateLimitIncomingMessageListener<>(5, 10000L);
         IncomingMessageResponseHandler mockedResponseHandler = Mockito.mock(IncomingMessageResponseHandler.class);
         
         IncomingMessage<Integer> messageFrom5 = new IncomingMessage<>(5, ByteBuffer.allocate(0), 0L);
         
         filter.messageArrived(messageFrom5, mockedResponseHandler);
         filter.messageArrived(messageFrom5, mockedResponseHandler);
         filter.messageArrived(messageFrom5, mockedResponseHandler);
         filter.messageArrived(messageFrom5, mockedResponseHandler);
         filter.messageArrived(messageFrom5, mockedResponseHandler);
         filter.messageArrived(messageFrom5, mockedResponseHandler);
     }

     @Test
     public void resetTest() throws InterruptedException {
         RateLimitIncomingMessageListener<Integer> filter = new RateLimitIncomingMessageListener<>(3, 300L);
         IncomingMessageResponseHandler mockedResponseHandler = Mockito.mock(IncomingMessageResponseHandler.class);
         
         IncomingMessage<Integer> messageFrom5 = new IncomingMessage<>(5, ByteBuffer.allocate(0), 0L);
         
         filter.messageArrived(messageFrom5, mockedResponseHandler);
         filter.messageArrived(messageFrom5, mockedResponseHandler);
         filter.messageArrived(messageFrom5, mockedResponseHandler);
         Thread.sleep(350L); // +50L just to be safe
         filter.messageArrived(messageFrom5, mockedResponseHandler);
         filter.messageArrived(messageFrom5, mockedResponseHandler);
         filter.messageArrived(messageFrom5, mockedResponseHandler);
     }

     @Test
     public void clearBannedTest() throws InterruptedException {
         RateLimitIncomingMessageListener<Integer> filter = new RateLimitIncomingMessageListener<>(3, 10000L);
         IncomingMessageResponseHandler mockedResponseHandler = Mockito.mock(IncomingMessageResponseHandler.class);
         
         IncomingMessage<Integer> messageFrom5 = new IncomingMessage<>(5, ByteBuffer.allocate(0), 0L);
         
         filter.messageArrived(messageFrom5, mockedResponseHandler);
         filter.messageArrived(messageFrom5, mockedResponseHandler);
         filter.messageArrived(messageFrom5, mockedResponseHandler);
         boolean thrown = false;
         try {
             filter.messageArrived(messageFrom5, mockedResponseHandler);
         } catch (AddressBannedException e) {
             thrown = true;
         }
         Assert.assertTrue(thrown);
         
         filter.clearBanned();
         filter.messageArrived(messageFrom5, mockedResponseHandler);
         filter.messageArrived(messageFrom5, mockedResponseHandler);
         filter.messageArrived(messageFrom5, mockedResponseHandler);
     }
}
