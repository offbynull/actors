package com.offbynull.peernetic.core.actor.helpers;

import static com.offbynull.peernetic.core.actor.helpers.IdGenerator.MIN_SEED_SIZE;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public final class IdGeneratorTest {
    
    private IdGenerator idGen;
    
    @Before
    public void setUp() {
        idGen = new IdGenerator(new byte[MIN_SEED_SIZE]);
    }

    @Test
    public void mustGenerateUniqueIds() {
        String id1 = idGen.generate();
        String id2 = idGen.generate();
        
        assertEquals("vG2dRFdL2ck=", id1);
        assertEquals("p90NcldL2co=", id2);
    }
    
}
