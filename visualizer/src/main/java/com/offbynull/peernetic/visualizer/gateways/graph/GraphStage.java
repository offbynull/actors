/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.visualizer.gateways.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
import org.apache.commons.collections4.map.UnmodifiableMap;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GraphStage extends Stage {

    private static final Logger LOG = LoggerFactory.getLogger(GraphStage.class);
    
    private final BidiMap<String, Label> nodes = new DualHashBidiMap<>();
    private final BidiMap<ImmutablePair<String, String>, Line> edges = new DualHashBidiMap<>();
    private final MultiMap<Label, Line> anchors = new MultiValueMap<>();
    private Group graph;
    
    private final UnmodifiableMap<Class<?>, Function<Object, Runnable>> runnableGenerators;

    public GraphStage() {
        Map<Class<?>, Function<Object, Runnable>> generators = new HashMap<>();
        
        generators.put(AddNode.class, this::generateAddNodeCode);
        generators.put(MoveNode.class, this::generateMoveNodeCode);
        generators.put(StyleNode.class, this::generateStyleNodeCode);
        generators.put(RemoveNode.class, this::generateRemoveNodeCode);
        generators.put(AddEdge.class, this::generateAddEdgeCode);
        generators.put(StyleEdge.class, this::generateStyleEdgeCode);
        generators.put(RemoveEdge.class, this::generateRemoveEdgeCode);
        
        runnableGenerators = (UnmodifiableMap<Class<?>, Function<Object, Runnable>>) UnmodifiableMap.unmodifiableMap(generators);
        
        graph = new Group();
        
        setTitle("Graph");
        setWidth(450);
        setHeight(450);

        setOnCloseRequest(x -> hide());


        Scene scene = new Scene(graph, 1.0, 1.0, true, SceneAntialiasing.DISABLED);
        scene.setFill(Color.WHITESMOKE);
        setScene(scene);
        
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
    }
    
    List<Runnable> execute(Collection<Object> commands) {
        Validate.notNull(commands);
        Validate.noNullElements(commands);
        
        List<Runnable> runnables = new ArrayList<>(commands.size());
        for (Object command : commands) {
            Function<Object, Runnable> func = runnableGenerators.get(command.getClass());
            if (func != null) {
                Runnable runnable = func.apply(command);
                runnables.add(runnable);
            } else {
                LOG.warn("Generator not found for command: {}", command);
                continue;
            }
        }
        
        return runnables;
    }
    
    private Runnable generateAddNodeCode(Object msg) {
        Validate.notNull(msg);
        AddNode addNode = (AddNode) msg;
        
        String id = addNode.getId();

        return () -> {
            Label label = new Label(id);

            label.layoutXProperty().set(0);
            label.layoutYProperty().set(0);

            Label existingLabel = nodes.putIfAbsent(id, label);
            Validate.isTrue(existingLabel == null);

            graph.getChildren().add(label);
        };
    }

    private Runnable generateMoveNodeCode(Object msg) {
        Validate.notNull(msg);
        MoveNode moveNode = (MoveNode) msg;
        
        String id = moveNode.getId();
        double x = moveNode.getX();
        double y = moveNode.getY();

        return () -> {
            Label label = nodes.get(id);
            Validate.isTrue(label != null);
            label.layoutXProperty().set(x);
            label.layoutYProperty().set(y);
        };
    }
    
    private Runnable generateStyleNodeCode(Object msg) {
        Validate.notNull(msg);
        StyleNode styleNode = (StyleNode) msg;
        
        String id = styleNode.getId();
        String style = styleNode.getStyle();

        return () -> {
            Label label = nodes.get(id);
            Validate.isTrue(label != null);
            
            label.setStyle(style);
        };
    }

    @SuppressWarnings("unchecked")
    private Runnable generateRemoveNodeCode(Object msg) {
        Validate.notNull(msg);
        RemoveNode removeNode = (RemoveNode) msg;
        
        String id = removeNode.getId();

        return () -> {
            Label label = nodes.get(id);
            Validate.isTrue(label != null);
            graph.getChildren().remove(label);

            Collection<Line> lines = (Collection<Line>) anchors.remove(label);
            for (Line line : lines) {
                edges.removeValue(line);
                graph.getChildren().remove(line);
            }
        };
    }

    private Runnable generateAddEdgeCode(Object msg) {
        Validate.notNull(msg);
        AddEdge addEdge = (AddEdge) msg;
        
        String fromId = addEdge.getFromId();
        String toId = addEdge.getToId();

        return () -> {
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
        };
    }

    private Runnable generateStyleEdgeCode(Object msg) {
        Validate.notNull(msg);
        StyleEdge styleEdge = (StyleEdge) msg;
        
        String fromId = styleEdge.getFromId();
        String toId = styleEdge.getToId();
        String style = styleEdge.getStyle();

        return () -> {
            ImmutablePair<String, String> key = new ImmutablePair<>(fromId, toId);
            Line line = edges.get(key);
            Validate.isTrue(line != null);

            line.setStyle(style); // null is implicitly converted to an empty string
        };
    }
    
    private Runnable generateRemoveEdgeCode(Object msg) {
        Validate.notNull(msg);
        RemoveEdge removeEdge = (RemoveEdge) msg;
        
        String fromId = removeEdge.getFromId();
        String toId = removeEdge.getToId();

        return () -> {
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
        };
    }
}
