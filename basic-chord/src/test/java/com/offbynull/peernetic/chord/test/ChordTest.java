package com.offbynull.peernetic.chord.test;

import com.offbynull.peernetic.chord.Chord;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.offbynull.peernetic.chord.test.TestUtils.*;

public class ChordTest {
    
    public ChordTest() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    public void testJoining() throws IOException, InterruptedException {
        Chord nodeOne = startNetwork(3, 0x00L);
        Chord nodeTwo = joinNetwork(3, 0x01L, nodeOne);
        
        assertTrue(true);
        
        nodeOne.stop();
        nodeTwo.stop();
    }
}