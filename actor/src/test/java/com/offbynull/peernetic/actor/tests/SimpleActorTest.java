package com.offbynull.peernetic.actor.tests;

import junit.framework.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SimpleActorTest {
    
    public SimpleActorTest() {
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
    public void basicActorTest() throws Throwable {
        RequesterActor requesterActor = new RequesterActor();
        
        ResponseActor responseActor = new ResponseActor();
        responseActor.start();
        
        requesterActor.setFriend(responseActor.getEndpoint());
        requesterActor.start();
        
        Thread.sleep(1000L);
        
        Assert.assertEquals(50L, requesterActor.getNumber());
        
        requesterActor.stop();
        responseActor.stop();
    }
}
