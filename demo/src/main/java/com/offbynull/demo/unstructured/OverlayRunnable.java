package com.offbynull.demo.unstructured;

import com.offbynull.overlay.unstructured.Overlay;
import com.offbynull.overlay.unstructured.OverlayListener;
import com.offbynull.overlay.visualizer.Visualizer;
import com.offbynull.rpc.Rpc;
import com.offbynull.rpc.RpcConfig;
import com.offbynull.rpc.TransportFactory;
import org.apache.commons.lang3.Validate;

public class OverlayRunnable implements Runnable {

    private int selfAddress;
    private TransportFactory<Integer> transportFactory;
    private int bootstrap;
    private int maxInLinks;
    private int maxOutLinks;
    private Visualizer<Integer> visualizer;

    public OverlayRunnable(int selfAddress, TransportFactory<Integer> transportFactory, int bootstrap, int maxInLinks, int maxOutLinks,
            Visualizer<Integer> visualizer) {
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
            RpcConfig<Integer> rpcConfig = new RpcConfig<>();

//            SelfBlockId selfBlockId = new SelfBlockId();
//            IncomingFilter<Integer> inFilter = new SelfBlockIncomingFilter<>(selfBlockId);
//            OutgoingFilter<Integer> outFilter = new SelfBlockOutgoingFilter<>(selfBlockId);
//            rpcConfig.setIncomingFilters(Arrays.asList(inFilter));
//            rpcConfig.setOutgoingFilters(Arrays.asList(outFilter));
            rpc = new Rpc<>(transportFactory, rpcConfig);
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
                visualizer.addConnection(selfAddress, address);

            }
        }

        @Override
        public void linkBroken(final Integer address, LinkType type) {
            if (type == LinkType.OUTGOING) {
                visualizer.removeConnection(selfAddress, address);
            }
        }

    }

}
