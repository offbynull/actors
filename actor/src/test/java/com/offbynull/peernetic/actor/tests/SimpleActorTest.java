package com.offbynull.peernetic.actor.tests;

import com.offbynull.peernetic.actor.ActorRunner;
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
        RequestActor requestActor = new RequestActor();
        
        ResponseActor responseActor = new ResponseActor();
        ActorRunner responseActorRunner = ActorRunner.createAndStart(responseActor);
        
        requestActor.setFriend(responseActorRunner.getEndpoint());
        ActorRunner requestActorRunner = ActorRunner.createAndStart(requestActor);
        
        Thread.sleep(1000L);
        
        Assert.assertEquals(50L, requestActor.getNumber());
        
        requestActorRunner.stop();
        responseActorRunner.stop();
    }
}
