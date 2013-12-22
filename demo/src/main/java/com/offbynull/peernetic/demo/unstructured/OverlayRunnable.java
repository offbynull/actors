package com.offbynull.peernetic.demo.unstructured;

import com.offbynull.peernetic.overlay.unstructured.Overlay;
import com.offbynull.peernetic.overlay.unstructured.OverlayListener;
import com.offbynull.peernetic.rpc.Rpc;
import com.offbynull.peernetic.rpc.RpcConfig;
import com.offbynull.peernetic.rpc.TransportFactory;
import com.offbynull.peernetic.visualizer.AddEdgeCommand;
import com.offbynull.peernetic.visualizer.AddNodeCommand;
import com.offbynull.peernetic.visualizer.ChangeNodeCommand;
import com.offbynull.peernetic.visualizer.RemoveNodeCommand;
import com.offbynull.peernetic.visualizer.Visualizer;
import java.awt.Color;
import java.awt.Point;
import java.util.Random;
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

            Random random = new Random();
            visualizer.step("Adding node " + selfAddress,
                    new AddNodeCommand<>(selfAddress),
                    new ChangeNodeCommand(selfAddress, null, new Point(random.nextInt(500), random.nextInt(500)), Color.GREEN));

            Thread.sleep(Long.MAX_VALUE);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                overlay.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            visualizer.step("Removing node " + selfAddress,
                    new RemoveNodeCommand<>(selfAddress));
        }
    }

    private final class CustomOverlayListener implements OverlayListener<Integer> {

        @Override
        public void linkEstablished(final Integer address, LinkType type) {
            if (type == LinkType.OUTGOING) {
                visualizer.step(selfAddress + " attached to " + address,
                        new AddEdgeCommand<>(selfAddress, address));
            }
        }

        @Override
        public void linkBroken(final Integer address, LinkType type) {
            if (type == LinkType.OUTGOING) {
                visualizer.step(selfAddress + " detached from " + address,
                        new AddEdgeCommand<>(selfAddress, address));
            }
        }

    }

}
