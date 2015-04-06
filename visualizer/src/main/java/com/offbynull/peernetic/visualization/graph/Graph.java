package com.offbynull.peernetic.visualization.graph;

import java.util.Collection;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import static javafx.beans.binding.DoubleExpression.doubleExpression;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.shape.Line;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;

public final class Graph extends Group {

    private final BidiMap<String, Label> nodes;
    private final BidiMap<ImmutablePair<String, String>, Line> edges;
    private final MultiMap<Label, Line> anchors;

    public Graph() {
        nodes = new DualHashBidiMap<>();
        edges = new DualHashBidiMap<>();
        anchors = new MultiValueMap<>();
    }

    public void addNode(String id, double x, double y) {
        Validate.notNull(id);
        Validate.isTrue(Double.isFinite(x));
        Validate.isTrue(Double.isFinite(y));

        Platform.runLater(() -> {
            Label label = new Label(id);
            
            DoubleBinding centerXBinding = doubleExpression(doubleExpression(label.widthProperty().divide(2.0).negate())).add(x);
            DoubleBinding centerYBinding = doubleExpression(doubleExpression(label.heightProperty().divide(2.0).negate())).add(y);
            
            label.layoutXProperty().bind(centerXBinding);
            label.layoutYProperty().bind(centerYBinding);

            Label existingLabel = nodes.putIfAbsent(id, label);
            Validate.isTrue(existingLabel == null);

            getChildren().add(label);
        });
    }

    @SuppressWarnings("unchecked")
    public void removeNode(String id) {
        Validate.notNull(id);

        Platform.runLater(() -> {
            Label label = nodes.get(id);
            Validate.isTrue(label != null);
            getChildren().remove(label);

            Collection<Line> lines = (Collection<Line>) anchors.remove(label);
            for (Line line : lines) {
                edges.removeValue(line);
                getChildren().remove(line);
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

            Line existingLine = edges.putIfAbsent(new ImmutablePair<>(fromId, toId), line);
            Validate.isTrue(existingLine == null);

            anchors.put(fromLabel, line);
            anchors.put(toLabel, line);

            getChildren().add(0, line);
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
            anchors.removeMapping(fromLabel, line);
            anchors.removeMapping(toLabel, line);

            getChildren().remove(line);
        });
    }

}
