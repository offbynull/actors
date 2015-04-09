package com.offbynull.peernetic.gateways.visualizer;

import com.offbynull.peernetic.core.Shuttle;
import com.offbynull.peernetic.core.gateway.Gateway;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import org.apache.commons.lang3.Validate;

public final class GraphGateway implements Gateway {

    private GraphShuttle graphShuttle;

    public GraphGateway(String prefix) {
        Validate.notNull(prefix);
        graphShuttle = new GraphShuttle(prefix);
    }

    @Override
    public Shuttle getIncomingShuttle() {
        return graphShuttle;
    }

    @Override
    public void close() {
        Platform.exit();
    }

    public static void startApplication() {
        Validate.isTrue(GraphApplication.getInstance() == null);
        Thread thread = new Thread(() -> Application.launch(GraphApplication.class));
        thread.setDaemon(true);
        thread.setName(GraphGateway.class.getName() + " thread");
        thread.start();
        
        try {
            GraphApplication.awaitInstance(); // wait until application started
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static void exitApplication() {
        Platform.exit();
    }
}
