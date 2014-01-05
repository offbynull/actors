package com.offbynull.peernetic.overlay.unstructured;

import com.offbynull.peernetic.rpc.transport.transports.test.TestTransportFactory;
import com.offbynull.peernetic.rpc.Rpc;
import com.offbynull.peernetic.rpc.transport.transports.test.PerfectLine;
import com.offbynull.peernetic.rpc.transport.transports.test.TestHub;
import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

@Ignore
public class UnstructuredOverlayTest {

    private TestHub hub;

    public UnstructuredOverlayTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws Throwable {
        hub = new TestHub<>(new PerfectLine<Integer>());
        hub.start();
    }

    @After
    public void tearDown() throws Throwable {
        hub.stop();
    }

    @Test
    public void emptyAddressCacheNotifyOnEmptyCreateTest() throws Throwable {
        UnstructuredOverlayListener<Integer> listenerMock = Mockito.mock(UnstructuredOverlayListener.class);

        try (OverlayEntry entry = new OverlayEntry(0, listenerMock)) {
            Mockito.verify(listenerMock, Mockito.timeout(1000).times(1)).addressCacheEmpty(entry.getOverlay());
        }
    }

    @Test
    public void emptyAddressCacheNotifyOnNonEmptyCreateTest() throws Throwable {
        UnstructuredOverlayListener<Integer> listenerMock = Mockito.mock(UnstructuredOverlayListener.class);

        try (OverlayEntry entry = new OverlayEntry(0, listenerMock)) {
            entry.getOverlay().addToAddressCache(1);
            Mockito.verify(listenerMock, Mockito.timeout(1000).times(1)).addressCacheEmpty(entry.getOverlay());
        }
    }

    @Test
    public void createAndMaintainOutgoingLinksTest() throws Throwable {
        UnstructuredOverlayListener<Integer> listenerMock = Mockito.mock(UnstructuredOverlayListener.class);

        try (
            OverlayEntry entry0 = new OverlayEntry(0, listenerMock);
            OverlayEntry entry1 = new OverlayEntry(1, null);
            OverlayEntry entry2 = new OverlayEntry(2, null);
            OverlayEntry entry3 = new OverlayEntry(3, null);
            OverlayEntry entry4 = new OverlayEntry(4, null);
            OverlayEntry entry5 = new OverlayEntry(5, null);
        ) {
            entry0.getOverlay().addToAddressCache(1, 2, 3, 4, 5);
            
            Thread.sleep(1000L);
            
            Mockito.verify(listenerMock).linkCreated(entry0.getOverlay(), LinkType.OUTGOING, 1);
            Mockito.verify(listenerMock).linkCreated(entry0.getOverlay(), LinkType.OUTGOING, 2);
            Mockito.verify(listenerMock).linkCreated(entry0.getOverlay(), LinkType.OUTGOING, 3);
            Mockito.verify(listenerMock).linkCreated(entry0.getOverlay(), LinkType.OUTGOING, 4);
            Mockito.verify(listenerMock).linkCreated(entry0.getOverlay(), LinkType.OUTGOING, 5);
            
            Thread.sleep(2000L);
            
            Mockito.verify(listenerMock, Mockito.never()).linkDestroyed(entry0.getOverlay(), LinkType.OUTGOING, 1);
            Mockito.verify(listenerMock, Mockito.never()).linkDestroyed(entry0.getOverlay(), LinkType.OUTGOING, 2);
            Mockito.verify(listenerMock, Mockito.never()).linkDestroyed(entry0.getOverlay(), LinkType.OUTGOING, 3);
            Mockito.verify(listenerMock, Mockito.never()).linkDestroyed(entry0.getOverlay(), LinkType.OUTGOING, 4);
            Mockito.verify(listenerMock, Mockito.never()).linkDestroyed(entry0.getOverlay(), LinkType.OUTGOING, 5);  
        }
    }
    
    @Test
    public void createAndMaintainIncomingLinksTest() throws Throwable {
        UnstructuredOverlayListener<Integer> listenerMock = Mockito.mock(UnstructuredOverlayListener.class);

        try (
            OverlayEntry entry0 = new OverlayEntry(0, listenerMock);
            OverlayEntry entry1 = new OverlayEntry(1, null);
            OverlayEntry entry2 = new OverlayEntry(2, null);
            OverlayEntry entry3 = new OverlayEntry(3, null);
            OverlayEntry entry4 = new OverlayEntry(4, null);
            OverlayEntry entry5 = new OverlayEntry(5, null);
        ) {
            entry1.getOverlay().addToAddressCache(0);
            entry2.getOverlay().addToAddressCache(0);
            entry3.getOverlay().addToAddressCache(0);
            entry4.getOverlay().addToAddressCache(0);
            entry5.getOverlay().addToAddressCache(0);

            Thread.sleep(1000L);
                        
            Mockito.verify(listenerMock).linkCreated(entry0.getOverlay(), LinkType.INCOMING, 1);
            Mockito.verify(listenerMock).linkCreated(entry0.getOverlay(), LinkType.INCOMING, 2);
            Mockito.verify(listenerMock).linkCreated(entry0.getOverlay(), LinkType.INCOMING, 3);
            Mockito.verify(listenerMock).linkCreated(entry0.getOverlay(), LinkType.INCOMING, 4);
            Mockito.verify(listenerMock).linkCreated(entry0.getOverlay(), LinkType.INCOMING, 5);
            
            Thread.sleep(2000L);
            
            Mockito.verify(listenerMock, Mockito.never()).linkDestroyed(entry0.getOverlay(), LinkType.INCOMING, 1);
            Mockito.verify(listenerMock, Mockito.never()).linkDestroyed(entry0.getOverlay(), LinkType.INCOMING, 2);
            Mockito.verify(listenerMock, Mockito.never()).linkDestroyed(entry0.getOverlay(), LinkType.INCOMING, 3);
            Mockito.verify(listenerMock, Mockito.never()).linkDestroyed(entry0.getOverlay(), LinkType.INCOMING, 4);
            Mockito.verify(listenerMock, Mockito.never()).linkDestroyed(entry0.getOverlay(), LinkType.INCOMING, 5);            
        }
    }

    @Test
    public void createAndDestroyOutgoingLinksTest() throws Throwable {
        UnstructuredOverlayListener<Integer> listenerMock = Mockito.mock(UnstructuredOverlayListener.class);

        try (
            OverlayEntry entry0 = new OverlayEntry(0, listenerMock);
            OverlayEntry entry1 = new OverlayEntry(1, null);
            OverlayEntry entry2 = new OverlayEntry(2, null);
            OverlayEntry entry3 = new OverlayEntry(3, null);
            OverlayEntry entry4 = new OverlayEntry(4, null);
            OverlayEntry entry5 = new OverlayEntry(5, null);
        ) {
            entry0.getOverlay().addToAddressCache(1, 2, 3, 4, 5);
            
            Thread.sleep(1000L);
            
            Mockito.verify(listenerMock).linkCreated(entry0.getOverlay(), LinkType.OUTGOING, 1);
            Mockito.verify(listenerMock).linkCreated(entry0.getOverlay(), LinkType.OUTGOING, 2);
            Mockito.verify(listenerMock).linkCreated(entry0.getOverlay(), LinkType.OUTGOING, 3);
            Mockito.verify(listenerMock).linkCreated(entry0.getOverlay(), LinkType.OUTGOING, 4);
            Mockito.verify(listenerMock).linkCreated(entry0.getOverlay(), LinkType.OUTGOING, 5);
            
            entry2.close();
            entry3.close();
            entry4.close();
            
            Thread.sleep(1000L);
            
            Mockito.verify(listenerMock, Mockito.never()).linkDestroyed(entry0.getOverlay(), LinkType.OUTGOING, 1);
            Mockito.verify(listenerMock).linkDestroyed(entry0.getOverlay(), LinkType.OUTGOING, 2);
            Mockito.verify(listenerMock).linkDestroyed(entry0.getOverlay(), LinkType.OUTGOING, 3);
            Mockito.verify(listenerMock).linkDestroyed(entry0.getOverlay(), LinkType.OUTGOING, 4);
            Mockito.verify(listenerMock, Mockito.never()).linkDestroyed(entry0.getOverlay(), LinkType.OUTGOING, 5);
        }
    }

    @Test
    public void createAndDestroyIncomingLinksTest() throws Throwable {
        UnstructuredOverlayListener<Integer> listenerMock = Mockito.mock(UnstructuredOverlayListener.class);

        try (
            OverlayEntry entry0 = new OverlayEntry(0, listenerMock);
            OverlayEntry entry1 = new OverlayEntry(1, null);
            OverlayEntry entry2 = new OverlayEntry(2, null);
            OverlayEntry entry3 = new OverlayEntry(3, null);
            OverlayEntry entry4 = new OverlayEntry(4, null);
            OverlayEntry entry5 = new OverlayEntry(5, null);
        ) {
            entry1.getOverlay().addToAddressCache(0);
            entry2.getOverlay().addToAddressCache(0);
            entry3.getOverlay().addToAddressCache(0);
            entry4.getOverlay().addToAddressCache(0);
            entry5.getOverlay().addToAddressCache(0);
            
            Thread.sleep(1000L);
            
            Mockito.verify(listenerMock).linkCreated(entry0.getOverlay(), LinkType.INCOMING, 1);
            Mockito.verify(listenerMock).linkCreated(entry0.getOverlay(), LinkType.INCOMING, 2);
            Mockito.verify(listenerMock).linkCreated(entry0.getOverlay(), LinkType.INCOMING, 3);
            Mockito.verify(listenerMock).linkCreated(entry0.getOverlay(), LinkType.INCOMING, 4);
            Mockito.verify(listenerMock).linkCreated(entry0.getOverlay(), LinkType.INCOMING, 5);
            
            entry2.close();
            entry3.close();
            entry4.close();
            
            Thread.sleep(1000L);
            
            Mockito.verify(listenerMock, Mockito.never()).linkDestroyed(entry0.getOverlay(), LinkType.INCOMING, 1);
            Mockito.verify(listenerMock).linkDestroyed(entry0.getOverlay(), LinkType.INCOMING, 2);
            Mockito.verify(listenerMock).linkDestroyed(entry0.getOverlay(), LinkType.INCOMING, 3);
            Mockito.verify(listenerMock).linkDestroyed(entry0.getOverlay(), LinkType.INCOMING, 4);
            Mockito.verify(listenerMock, Mockito.never()).linkDestroyed(entry0.getOverlay(), LinkType.INCOMING, 5);
        }
    }

    private final class OverlayEntry implements AutoCloseable {

        private Rpc<Integer> rpc;
        private UnstructuredOverlay<Integer> overlay;

        public OverlayEntry(int address, UnstructuredOverlayListener<Integer> listener) throws IOException {
            TestTransportFactory<Integer> transportFactory = new TestTransportFactory<>(hub, address);
            transportFactory.setIncomingResponseTimeout(10000L);
            transportFactory.setOutgoingResponseTimeout(10000L);
            rpc = new Rpc<>(transportFactory);

            if (listener == null) {
                listener = new UnstructuredOverlayListener<Integer>() {

                    @Override
                    public void linkCreated(UnstructuredOverlay<Integer> overlay, LinkType type, Integer address) {
                    }

                    @Override
                    public void linkDestroyed(UnstructuredOverlay<Integer> overlay, LinkType type, Integer address) {
                    }

                    @Override
                    public void addressCacheEmpty(UnstructuredOverlay<Integer> overlay) {
                    }
                };
            }
            overlay = new UnstructuredOverlay(rpc, listener, 5, 5, 5, 500L, 1000L, 1000L, 1000L);
            overlay.start();
        }

        public UnstructuredOverlay<Integer> getOverlay() {
            return overlay;
        }

        @Override
        public void close() {
            try {
                overlay.stop();
            } catch (RuntimeException re) {
                // do nothing
            }

            try {
                rpc.close();
            } catch (IOException ioe) {
                // do nothing
            }
        }
    }
}
