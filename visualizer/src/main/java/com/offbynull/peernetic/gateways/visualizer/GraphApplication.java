package com.offbynull.peernetic.gateways.visualizer;

import java.util.Collection;
import java.util.concurrent.Phaser;
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
import javafx.stage.Stage;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;

public final class GraphApplication extends Application {

    private static GraphApplication instance;
    private static final Lock readyLock = new ReentrantLock();
    private static final Condition readyCondition = readyLock.newCondition();
    
    private final BidiMap<String, Label> nodes = new DualHashBidiMap<>();
    private final BidiMap<ImmutablePair<String, String>, Line> edges = new DualHashBidiMap<>();
    private final MultiMap<Label, Line> anchors = new MultiValueMap<>();
    private Group graph;

    @Override
    public void init() throws Exception {
        readyLock.lock();
        try {
            instance = this;
            readyCondition.signal();
        } finally {
            readyLock.unlock();
        }
    }

    @Override
    public void start(Stage stage) {
        graph = new Group();
        
        stage.setTitle("Graph");
        stage.setWidth(700);
        stage.setHeight(700);


        Scene scene = new Scene(graph, 1.0, 1.0, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.WHITESMOKE);
        stage.setScene(scene);
        
        InvalidationListener invalidationListener = (x) -> {
            double scaleX = scene.getWidth() / graph.getLayoutBounds().getWidth();
            double scaleY = scene.getHeight() / graph.getLayoutBounds().getHeight();
            double layoutX = (scene.getWidth() / 2.0) - (graph.getLayoutBounds().getWidth() / 2.0);
            double layoutY = (scene.getHeight() / 2.0) - (graph.getLayoutBounds().getHeight() / 2.0);
            graph.setScaleX(scaleX);
            graph.setScaleY(scaleY);
            graph.setLayoutX(layoutX);
            graph.setLayoutY(layoutY);
        };
        
        graph.layoutBoundsProperty().addListener(invalidationListener);
        scene.widthProperty().addListener(invalidationListener);
        scene.heightProperty().addListener(invalidationListener);
        
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        readyLock.lock();
        try {
            instance = null;
        } finally {
            readyLock.unlock();
        }
    }

    public static GraphApplication getInstance() {
        readyLock.lock();
        try {
            return instance;
        } finally {
            readyLock.unlock();
        }
    }
    
    public static GraphApplication awaitInstance() throws InterruptedException {
        readyLock.lock();
        try {
            if (instance == null) {
                readyCondition.await();
            }
            return instance;
        } finally {
            readyLock.unlock();
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

            DoubleBinding fromCenterXBinding = doubleExpression(doubleExpression(fromLabel.widthProperty().divide(2.0).negate()))
                    .add(fromLabel.layoutXProperty());
            DoubleBinding fromCenterYBinding = doubleExpression(doubleExpression(fromLabel.heightProperty().divide(2.0).negate()))
                    .add(fromLabel.layoutYProperty());
            DoubleBinding toCenterXBinding = doubleExpression(doubleExpression(toLabel.widthProperty().divide(2.0).negate()))
                    .add(toLabel.layoutXProperty());
            DoubleBinding toCenterYBinding = doubleExpression(doubleExpression(toLabel.heightProperty().divide(2.0).negate()))
                    .add(toLabel.layoutYProperty());
            
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

            Line existingLine = edges.putIfAbsent(new ImmutablePair<>(fromId, toId), line);
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
            Line line = edges.remove(new ImmutablePair<>(fromId, toId));
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

            Line line = edges.remove(new ImmutablePair<>(fromId, toId));
            Validate.isTrue(line != null);
            
            anchors.removeMapping(fromLabel, line);
            anchors.removeMapping(toLabel, line);

            graph.getChildren().remove(line);
        });
    }
}
