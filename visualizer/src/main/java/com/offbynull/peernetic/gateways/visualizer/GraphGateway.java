package com.offbynull.peernetic.gateways.visualizer;

import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.gateway.InputGateway;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import com.offbynull.peernetic.core.shuttles.simple.SimpleShuttle;
import java.util.function.Supplier;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.commons.lang3.Validate;

public final class GraphGateway implements InputGateway {

    private final Thread thread;
    private final Bus bus;
    
    private final SimpleShuttle shuttle;

    public GraphGateway(String prefix) {
        Validate.notNull(prefix);

        bus = new Bus();
        shuttle = new SimpleShuttle(prefix, bus);
        thread = new Thread(new GraphRunnable(bus));
        thread.setDaemon(true);
        thread.setName(getClass().getSimpleName() + "-" + prefix);
        thread.start();
    }

    @Override
    public Shuttle getIncomingShuttle() {
        return shuttle;
    }

    @Override
    public void close() {
        thread.interrupt();
        exitApplication();
    }

    public static void startApplication() {
        Validate.isTrue(GraphApplication.getInstance() == null);
        Thread thread = new Thread(() -> Application.launch(GraphApplication.class));
        thread.setDaemon(true);
        thread.setName(GraphGateway.class.getName() + " thread");
        thread.start();
        
        try {
            GraphApplication.awaitStarted();
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    public void addStage(Supplier<? extends Stage> factory) {
        Validate.notNull(factory);
        bus.add(new CreateStage(factory));
    }

    public static void awaitShutdown() {
        try {
            GraphApplication.awaitStopped();
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static void exitApplication() {
        Platform.exit();
    }
}
