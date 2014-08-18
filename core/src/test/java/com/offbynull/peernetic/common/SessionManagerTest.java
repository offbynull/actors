package com.offbynull.peernetic.common;

import java.time.Duration;
import java.time.Instant;
import org.junit.Assert;
import org.junit.Test;

public final class SessionManagerTest {
    private static final String SESSION_1_ID = "session1";
    private static final String SESSION_2_ID = "session2";
    private static final String SESSION_1_PARAMS = "params";
    
    @Test
    public void basicSessionManagerTest() {
        SessionManager<String> sessionManager = new SessionManager<>();
        
        Instant startTime = Instant.ofEpochMilli(0L);
        Instant nextTime = startTime;
        
        Assert.assertFalse(sessionManager.containsSession(SESSION_1_ID));
        Assert.assertFalse(sessionManager.containsSession(SESSION_2_ID));
        
        sessionManager.addSession(nextTime, Duration.ofSeconds(5L), SESSION_1_ID, SESSION_1_PARAMS);
        sessionManager.addSession(nextTime, Duration.ofSeconds(10L), SESSION_2_ID, null);
        Assert.assertEquals(SESSION_1_PARAMS, sessionManager.getSessionParam(SESSION_1_ID));
        Assert.assertTrue(sessionManager.containsSession(SESSION_1_ID));
        Assert.assertTrue(sessionManager.containsSession(SESSION_2_ID));
        
        nextTime = startTime.plusSeconds(1L);
        sessionManager.addOrUpdateSession(nextTime, Duration.ofSeconds(5L), SESSION_1_ID, SESSION_1_PARAMS);
        sessionManager.process(nextTime);
        Assert.assertTrue(sessionManager.containsSession(SESSION_1_ID));
        Assert.assertTrue(sessionManager.containsSession(SESSION_2_ID));
        
        nextTime = startTime.plusSeconds(2L);
        sessionManager.process(nextTime);
        Assert.assertTrue(sessionManager.containsSession(SESSION_1_ID));
        Assert.assertTrue(sessionManager.containsSession(SESSION_2_ID));
        
        nextTime = startTime.plusSeconds(5L);
        sessionManager.process(nextTime);
        Assert.assertTrue(sessionManager.containsSession(SESSION_1_ID));
        Assert.assertTrue(sessionManager.containsSession(SESSION_2_ID));
        
        nextTime = startTime.plusSeconds(6L);
        sessionManager.process(nextTime);
        Assert.assertFalse(sessionManager.containsSession(SESSION_1_ID));
        Assert.assertTrue(sessionManager.containsSession(SESSION_2_ID));
    }
}
