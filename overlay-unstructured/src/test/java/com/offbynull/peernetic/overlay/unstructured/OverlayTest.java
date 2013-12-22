package com.offbynull.peernetic.overlay.unstructured;

import com.offbynull.peernetic.overlay.unstructured.OverlayListener;
import com.offbynull.peernetic.overlay.unstructured.Overlay;
import com.offbynull.peernetic.rpc.FakeTransportFactory;
import com.offbynull.peernetic.rpc.Rpc;
import com.offbynull.peernetic.rpc.transport.fake.FakeHub;
import com.offbynull.peernetic.rpc.transport.fake.PerfectLine;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class OverlayTest {
    
    private FakeHub<Integer> hub;
    
    public OverlayTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() throws IOException {
        hub = new FakeHub<>(new PerfectLine<Integer>());
        hub.start();
    }
    
    @After
    public void tearDown() {
        hub.stop();
    }

     @Test
     public void twoNodeTest() throws Throwable {         
         List<Rpc<Integer>> rpcs = createRpcs(4);
         List<Overlay<Integer>> overlays = createOverlays(rpcs, 3, 3);
         
         Thread.sleep(5000L);
         
         
         destroyOverlays(overlays);
         destroyRpcs(rpcs);
     }
     
     
     private List<Rpc<Integer>> createRpcs(int count) throws IOException {
         List<Rpc<Integer>> ret = new ArrayList<>(count);
         
         for (int i = 0; i < count; i++) {
             FakeTransportFactory<Integer> transportFactory = new FakeTransportFactory<>(hub, i);
             Rpc rpc = new Rpc(transportFactory);
             
             ret.add(rpc);
         }
         
         return ret;
     }

     private List<Overlay<Integer>> createOverlays(List<Rpc<Integer>> rpcs, int maxInLinks, int maxOutLinks) throws InterruptedException {
         Random random = new Random(12345L);
         List<Overlay<Integer>> ret = new ArrayList<>(rpcs.size());
         
         for (int i = 0; i < rpcs.size(); i++) {
             Rpc<Integer> rpc = rpcs.get(i);
             
             Overlay<Integer> overlay = new Overlay<>();
             
             // get a random bootstrap node
             int bootstrap;
             do {
                 bootstrap = random.nextInt(rpcs.size());
             } while (bootstrap == i);
             
             final int selfAddress = i;
             overlay.start(rpc, bootstrap, maxInLinks, maxOutLinks, new OverlayListener<Integer>() {

                 @Override
                 public void linkEstablished(Integer address, LinkType type) {
                     switch (type) {
                         case INCOMING:
                             System.out.println(selfAddress + " incoming connected from " + address);
                             break;
                         case OUTGOING:
                             System.out.println(selfAddress + " outgoing connected to " + address);
                             break;
                         default:
                             throw new IllegalStateException();
                     }
                 }

                 @Override
                 public void linkBroken(Integer address, LinkType type) {
                     switch (type) {
                         case INCOMING:
                             System.out.println(selfAddress + " incoming broken from " + address);
                             break;
                         case OUTGOING:
                             System.out.println(selfAddress + " outgoing broken to " + address);
                             break;
                         default:
                             throw new IllegalStateException();
                     }
                 }
             });
             
             ret.add(overlay);
         }
         
         return ret;
     }

     private void destroyOverlays(List<Overlay<Integer>> overlays) throws InterruptedException {
         for (int i = 0; i < overlays.size(); i++) {
             Overlay<Integer> overlay = overlays.get(i);
             overlay.stop();
         }
     }

     private void destroyRpcs(List<Rpc<Integer>> rpcs) {
         for (Rpc<Integer> rpc : rpcs) {
             try {
                rpc.close();
             } catch (IOException ioe) {
                 // do nothing
             }
         }
     }
}
