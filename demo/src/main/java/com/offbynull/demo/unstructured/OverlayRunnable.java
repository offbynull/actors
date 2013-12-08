package com.offbynull.demo.unstructured;

import com.offbynull.overlay.unstructured.Overlay;
import com.offbynull.overlay.unstructured.OverlayListener;
import com.offbynull.overlay.visualizer.VisualizeComponent;
import com.offbynull.rpc.Rpc;
import com.offbynull.rpc.RpcConfig;
import com.offbynull.rpc.TransportFactory;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.Validate;

public class OverlayRunnable implements Runnable {
    
    private int selfAddress;
    private TransportFactory<Integer> transportFactory;
    private int bootstrap;
    private int maxInLinks;
    private int maxOutLinks;
    private VisualizeComponent<Integer> visualizer;

    public OverlayRunnable(int selfAddress, TransportFactory<Integer> transportFactory, int bootstrap, int maxInLinks, int maxOutLinks,
            VisualizeComponent<Integer> visualizer) {
        Validate.notNull(transportFactory);
        Validate.notNull(visualizer);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, maxInLinks);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, maxOutLinks);
        
        this.selfAddress = selfAddress;
        this.transportFactory = transportFactory;
        this.bootstrap = bootstrap;
        this.maxInLinks = maxInLinks;
        this.maxOutLinks = maxOutLinks;
        this.visualizer = visualizer;
    }


    @Override
    public void run() {
        Rpc<Integer> rpc = null;
        Overlay<Integer> overlay = null;
        
        try {
            rpc = new Rpc<>(transportFactory, new RpcConfig<Integer>());
            overlay = new Overlay<>();
            overlay.start(rpc, bootstrap, maxInLinks, maxOutLinks, new CustomOverlayListener());
            
            visualizer.addNode(selfAddress);

            Thread.sleep(Long.MAX_VALUE);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                overlay.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            visualizer.removeNode(selfAddress);
        }
    }

    private final class CustomOverlayListener implements OverlayListener<Integer> {

        @Override
        public void linkEstablished(final Integer address, LinkType type) {
            if (type == LinkType.OUTGOING) {
                System.out.println("CREATED " + selfAddress + "/" + address);
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        visualizer.addConnection(selfAddress, address);
                    }
                });
            }
        }

        @Override
        public void linkBroken(final Integer address, LinkType type) {
            if (type == LinkType.OUTGOING) {
                System.out.println("REMOVED " + selfAddress + "/" + address);
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        visualizer.removeConnection(selfAddress, address);
                    }
                });
            }
        }

    }

}
