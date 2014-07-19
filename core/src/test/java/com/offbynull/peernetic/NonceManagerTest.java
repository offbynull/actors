package com.offbynull.peernetic;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;

public class NonceManagerTest {
    
    private static final String NONCE_1_RESPONSE = "resp1";
    private static final String NONCE_2_RESPONSE = "resp2";
    
    @Test
    public void basicNonceManagerTest() {
        NonceGenerator<byte[]> nonceGenerator = new ByteArrayNonceGenerator(16);
        NonceManager<byte[]> nonceManager = new NonceManager<>();
        
        Nonce<byte[]> nonce1 = nonceGenerator.generate();
        Nonce<byte[]> nonce2 = nonceGenerator.generate();
        
        Instant startTime = Instant.ofEpochMilli(0L);
        Instant nextTime = startTime;
        
        Assert.assertNull(nonceManager.checkNonce(nextTime, nonce1));
        Assert.assertNull(nonceManager.checkNonce(nextTime, nonce2));
        
        nonceManager.addNonce(nextTime, Duration.ofSeconds(5L), nonce1, NONCE_1_RESPONSE);
        nonceManager.addNonce(nextTime, Duration.ofSeconds(10L), nonce2, null);
        Assert.assertEquals(NONCE_1_RESPONSE, nonceManager.checkNonce(nextTime, nonce1).get());
        Assert.assertEquals(Optional.empty(), nonceManager.checkNonce(nextTime, nonce2));

        nonceManager.assignResponse(nextTime, nonce2, NONCE_2_RESPONSE);
        Assert.assertEquals(NONCE_1_RESPONSE, nonceManager.checkNonce(nextTime, nonce1).get());
        Assert.assertEquals(NONCE_2_RESPONSE, nonceManager.checkNonce(nextTime, nonce2).get());
        
        nextTime = startTime.plusSeconds(1L);
        Assert.assertEquals(NONCE_1_RESPONSE, nonceManager.checkNonce(nextTime, nonce1).get());
        Assert.assertEquals(NONCE_2_RESPONSE, nonceManager.checkNonce(nextTime, nonce2).get());
        
        nextTime = startTime.plusSeconds(2L);
        nonceManager.prune(nextTime);
        Assert.assertEquals(NONCE_1_RESPONSE, nonceManager.checkNonce(nextTime, nonce1).get());
        Assert.assertEquals(NONCE_2_RESPONSE, nonceManager.checkNonce(nextTime, nonce2).get());
        
        nextTime = startTime.plusSeconds(5L);
        nonceManager.prune(nextTime);
        Assert.assertNull(NONCE_1_RESPONSE, nonceManager.checkNonce(nextTime, nonce1));
        Assert.assertEquals(NONCE_2_RESPONSE, nonceManager.checkNonce(nextTime, nonce2).get());
    }
}
