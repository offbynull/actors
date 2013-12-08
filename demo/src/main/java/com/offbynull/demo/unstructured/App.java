package com.offbynull.demo.unstructured;

import com.offbynull.overlay.visualizer.VisualizeComponent;
import com.offbynull.overlay.visualizer.VisualizeUtils;
import com.offbynull.rpc.FakeTransportFactory;
import com.offbynull.rpc.transport.fake.FakeHub;
import com.offbynull.rpc.transport.fake.PerfectLine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App {

    public static void main(String[] args) throws Throwable {
        VisualizeComponent<Integer> visualizer = new VisualizeComponent<>();
        VisualizeUtils.displayInWindow("Unstructured Overlay", visualizer);
        
        FakeHub<Integer> hub = new FakeHub<>(new PerfectLine<Integer>());
        hub.start();
        
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            executor.submit(new OverlayRunnable(i, new FakeTransportFactory<>(hub, i), i+1, 3, 3, visualizer));
        }
    }
}
