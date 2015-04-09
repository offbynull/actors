package com.offbynull.peernetic.visualization;

import com.offbynull.peernetic.core.Shuttle;
import com.offbynull.peernetic.core.gateway.Gateway;
import javafx.application.Application;
import javafx.application.Platform;
import org.apache.commons.lang3.Validate;

public final class GraphGateway implements Gateway {

    private final static Object LOCK = new Object();
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
        synchronized(LOCK) {
            Validate.isTrue(GraphApplication.getInstance() == null);
            Thread thread = new Thread(() -> Application.launch(GraphApplication.class));
            thread.setDaemon(true);
            thread.setName(GraphGateway.class.getName() + " thread");
            thread.start();
        }
    }

    public static void exitApplication() {
        synchronized(LOCK) {
            Platform.exit();
        }
    }
}
