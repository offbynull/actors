package com.offbynull.peernetic.gateways.visualizer;

import java.util.Collection;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.DoubleBinding;
import static javafx.beans.binding.DoubleExpression.doubleExpression;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;

public final class GraphApplication extends Application {

    private static volatile GraphApplication instance;
    private static final Lock lock = new ReentrantLock();
    private static final Condition startedCondition = lock.newCondition();
    private static final Condition stoppedCondition = lock.newCondition();
    
    private final BidiMap<String, Label> nodes = new DualHashBidiMap<>();
    private final BidiMap<ImmutablePair<String, String>, Line> edges = new DualHashBidiMap<>();
    private final MultiMap<Label, Line> anchors = new MultiValueMap<>();
    private Group graph;

    @Override
    public void init() throws Exception {
        Platform.setImplicitExit(true);
        
        lock.lock();
        try {
            instance = this;
            startedCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void start(Stage stage) {
        graph = new Group();
        
        stage.setTitle("Graph");
        stage.setWidth(700);
        stage.setHeight(700);
        
        // We have to do this because in some cases JavaFX threads won't shut down when the last stage closes, even if implicitExit is set
        // http://stackoverflow.com/questions/15808063/how-to-stop-javafx-application-thread/22997736#22997736
        stage.setOnCloseRequest(x -> Platform.exit());


        Scene scene = new Scene(graph, 1.0, 1.0, true, SceneAntialiasing.DISABLED);
        scene.setFill(Color.WHITESMOKE);
        stage.setScene(scene);
        
        InvalidationListener invalidationListener = (x) -> {
            double scaleX = scene.getWidth() / graph.getLayoutBounds().getWidth();
            double scaleY = scene.getHeight() / graph.getLayoutBounds().getHeight();
            graph.getTransforms().clear();
            graph.getTransforms().add(new Scale(scaleX, scaleY, 0.0, 0.0));
            graph.getTransforms().add(new Translate(-graph.getLayoutBounds().getMinX(), -graph.getLayoutBounds().getMinY()));
        };
        
        graph.layoutBoundsProperty().addListener(invalidationListener);
        scene.widthProperty().addListener(invalidationListener);
        scene.heightProperty().addListener(invalidationListener);
        
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        lock.lock();
        try {
            instance = null;
            stoppedCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public static GraphApplication getInstance() {
        lock.lock();
        try {
            return instance;
        } finally {
            lock.unlock();
        }
    }
    
    public static GraphApplication awaitStarted() throws InterruptedException {
        lock.lock();
        try {
            if (instance == null) {
                startedCondition.await();
            }
            return instance;
        } finally {
            lock.unlock();
        }
    }

    public static void awaitStopped() throws InterruptedException {
        lock.lock();
        try {
            if (instance != null) {
                stoppedCondition.await();
            }
        } finally {
            lock.unlock();
        }
    }
    
    public void addNode(String id, double x, double y, String style) {
        Validate.notNull(id);
        Validate.isTrue(Double.isFinite(x));
        Validate.isTrue(Double.isFinite(y));
        Validate.notNull(style);

        Platform.runLater(() -> {
            Label label = new Label(id);

            label.layoutXProperty().set(x);
            label.layoutYProperty().set(y);
            label.setStyle(style);

            Label existingLabel = nodes.putIfAbsent(id, label);
            Validate.isTrue(existingLabel == null);

            graph.getChildren().add(label);
        });
    }

    public void moveNode(String id, double x, double y) {
        Validate.notNull(id);
        Validate.isTrue(Double.isFinite(x));
        Validate.isTrue(Double.isFinite(y));

        Platform.runLater(() -> {
            Label label = nodes.get(id);
            Validate.isTrue(label != null);
            label.layoutXProperty().set(x);
            label.layoutYProperty().set(y);
        });
    }
    
    public void styleNode(String id, String style) {
        Validate.notNull(id);
        Validate.notNull(style);

        Platform.runLater(() -> {
            Label label = nodes.get(id);
            Validate.isTrue(label != null);
            
            label.setStyle(style);
        });
    }

    @SuppressWarnings("unchecked")
    public void removeNode(String id) {
        Validate.notNull(id);

        Platform.runLater(() -> {
            Label label = nodes.get(id);
            Validate.isTrue(label != null);
            graph.getChildren().remove(label);

            Collection<Line> lines = (Collection<Line>) anchors.remove(label);
            for (Line line : lines) {
                edges.removeValue(line);
                graph.getChildren().remove(line);
            }
        });
    }

    public void addEdge(String fromId, String toId) {
        Validate.notNull(fromId);
        Validate.notNull(toId);

        Platform.runLater(() -> {
            Label fromLabel = nodes.get(fromId);
            Label toLabel = nodes.get(toId);

            Validate.notNull(fromLabel);
            Validate.notNull(toLabel);
            
            Line line = new Line();
            DoubleBinding fromXBinding = doubleExpression(fromLabel.layoutXProperty())
                    .add(doubleExpression(fromLabel.widthProperty()).divide(2.0));
            DoubleBinding fromYBinding = doubleExpression(fromLabel.layoutYProperty())
                    .add(doubleExpression(fromLabel.heightProperty()).divide(2.0));
            DoubleBinding toXBinding = doubleExpression(toLabel.layoutXProperty())
                    .add(doubleExpression(toLabel.widthProperty()).divide(2.0));
            DoubleBinding toYBinding = doubleExpression(toLabel.layoutYProperty())
                    .add(doubleExpression(toLabel.heightProperty()).divide(2.0));
            line.startXProperty().bind(fromXBinding);
            line.startYProperty().bind(fromYBinding);
            line.endXProperty().bind(toXBinding);
            line.endYProperty().bind(toYBinding);

            ImmutablePair<String, String> key = new ImmutablePair<>(fromId, toId);
            Line existingLine = edges.putIfAbsent(key, line);
            Validate.isTrue(existingLine == null);

            anchors.put(fromLabel, line);
            anchors.put(toLabel, line);

            graph.getChildren().add(0, line);
        });
    }

    public void styleEdge(String fromId, String toId, String style) {
        Validate.notNull(fromId);
        Validate.notNull(toId);

        Platform.runLater(() -> {
            ImmutablePair<String, String> key = new ImmutablePair<>(fromId, toId);
            Line line = edges.get(key);
            Validate.isTrue(line != null);

            line.setStyle(style); // null is implicitly converted to an empty string
        });
    }
    
    public void removeEdge(String fromId, String toId) {
        Validate.notNull(fromId);
        Validate.notNull(toId);

        Platform.runLater(() -> {
            Label fromLabel = nodes.get(fromId);
            Label toLabel = nodes.get(toId);

            Validate.notNull(fromLabel);
            Validate.notNull(toLabel);

            ImmutablePair<String, String> key = new ImmutablePair<>(fromId, toId);
            Line line = edges.remove(key);
            Validate.isTrue(line != null);
            
            anchors.removeMapping(fromLabel, line);
            anchors.removeMapping(toLabel, line);

            graph.getChildren().remove(line);
        });
    }
}
