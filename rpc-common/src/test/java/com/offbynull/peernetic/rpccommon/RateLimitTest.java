package com.offbynull.peernetic.rpccommon;

import com.offbynull.peernetic.rpccommon.filters.accesscontrol.RateLimitIncomingFilter;
import com.offbynull.peernetic.rpccommon.filters.accesscontrol.RateLimitIncomingFilter.AddressBannedException;
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
