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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.DoubleBinding;
import static javafx.beans.binding.DoubleExpression.doubleExpression;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
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
    
    private final Map<String, NodeLabel> nodes = new HashMap<>();
    private final BidiMap<ImmutablePair<String, String>, EdgeLine> edges = new DualHashBidiMap<>();
    private final MultiMap<NodeLabel, EdgeLine> anchors = new MultiValueMap<>();
    private Group graph;
    
    private final UnmodifiableMap<Class<?>, Function<Object, Runnable>> runnableGenerators;

    public GraphStage() {
        Map<Class<?>, Function<Object, Runnable>> generators = new HashMap<>();
        
        generators.put(AddNode.class, this::generateAddNodeCode);
        generators.put(MoveNode.class, this::generateMoveNodeCode);
        generators.put(StyleNode.class, this::generateStyleNodeCode);
        generators.put(LabelNode.class, this::generateLabelNodeCode);
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
            NodeLabel label = nodes.get(id);
            
            if (label == null) {
                // Label doesn't exist, create it and add it in.
                label = new NodeLabel(id);
                label.layoutXProperty().set(0);
                label.layoutYProperty().set(0);
                graph.getChildren().add(label);
                nodes.put(id, label);
            } else {
                // Label does exist...
                //   if it is temporary than make it in to a permenant label
                //   if it isn't temporary, that means that it's already been added before, so throw exception
                Validate.isTrue(label.isTemporary(), "Node %s cannot be added because it already exists", id);
                label.setTemporary(false);
            }
        };
    }
    
    private Runnable generateMoveNodeCode(Object msg) {
        Validate.notNull(msg);
        MoveNode moveNode = (MoveNode) msg;
        
        String id = moveNode.getId();
        double x = moveNode.getX();
        double y = moveNode.getY();

        return () -> {
            NodeLabel label = nodes.get(id);
            Validate.isTrue(label != null, "Node %s cannot be moved because it doesn't exist", id);
            Validate.isTrue(!label.isTemporary(), "Node %s cannot be moved because it was never explicitly added", id);
            label.layoutXProperty().set(x);
            label.layoutYProperty().set(y);
        };
    }
    
    private Runnable generateStyleNodeCode(Object msg) {
        Validate.notNull(msg);
        StyleNode styleNode = (StyleNode) msg;
        
        String id = styleNode.getId();
        int color = styleNode.getColor();

        return () -> {
            NodeLabel label = nodes.get(id);
            Validate.isTrue(label != null, "Node %s cannot be styled because it doesn't exist", id);
            Validate.isTrue(!label.isTemporary(), "Node %s cannot be styled because it was never explicitly added", id);
            
            label.setColor(color); // automatically applies style based on color and temp
        };
    }
    
    private Runnable generateLabelNodeCode(Object msg) {
        Validate.notNull(msg);
        LabelNode labelNode = (LabelNode) msg;
        
        String id = labelNode.getId();
        String labelStr = labelNode.getLabel();

        return () -> {
            NodeLabel label = nodes.get(id);
            Validate.isTrue(label != null, "Node %s cannot be labeled because it doesn't exist", id);
            Validate.isTrue(!label.isTemporary(), "Node %s cannot be labeled because it was never explicitly added", id);
            
            label.setText(labelStr);
        };
    }

    @SuppressWarnings("unchecked")
    private Runnable generateRemoveNodeCode(Object msg) {
        Validate.notNull(msg);
        RemoveNode removeNode = (RemoveNode) msg;
        
        String id = removeNode.getId();
        boolean removeAsFrom = removeNode.isRemoveAsFrom();
        boolean removeAsTo = removeNode.isRemoveAsTo();

        return () -> {
            NodeLabel label = nodes.get(id);
            Validate.isTrue(label != null, "Node %s cannot be removed because it doesn't exist", id);
            Validate.isTrue(!label.isTemporary(), "Node %s cannot be removed because it was never explicitly added", id);

            Collection<EdgeLine> lines = (Collection<EdgeLine>) anchors.get(label);
            
            // If edges are present...
            if (lines != null && !lines.isEmpty()) {
                // If this label should attempt to remove edges where this node is the source, do so
                if (removeAsFrom) {
                    Iterator<EdgeLine> it = lines.iterator();
                    while (it.hasNext()) {
                        EdgeLine line = it.next();
                        ImmutablePair<String, String> conn = edges.getKey(line);
                        if (conn.getLeft().equals(id)) {
                            it.remove();
                            edges.removeValue(line);
                            graph.getChildren().remove(line);
                        }
                    }
                }

                // If this label should attempt to remove edges where this node is the destination, do so
                if (removeAsTo) {
                    Iterator<EdgeLine> it = lines.iterator();
                    while (it.hasNext()) {
                        EdgeLine line = it.next();
                        ImmutablePair<String, String> conn = edges.getKey(line);
                        if (conn.getRight().equals(id)) {
                            it.remove();
                            edges.removeValue(line);
                            graph.getChildren().remove(line);
                        }
                    }
                }
            }

            // If there's still stuff pointing to/from this node, mark it as temporary (once all the edges get removed the node will
            // disappear). Otherwise, remove it immediately.
            lines = (Collection<EdgeLine>) anchors.get(label); // reget just in case ... depending on implementation this may be a backing
                                                               // collection
            if (lines != null && !lines.isEmpty()) {
                label.setTemporary(true);
            } else {
                anchors.remove(label);
                nodes.remove(id);
                graph.getChildren().remove(label);
            }
        };
    }

    private Runnable generateAddEdgeCode(Object msg) {
        Validate.notNull(msg);
        AddEdge addEdge = (AddEdge) msg;
        
        String fromId = addEdge.getFromId();
        String toId = addEdge.getToId();

        return () -> {
            NodeLabel fromLabel = createTempNodeIfDoesNotExist(fromId);
            NodeLabel toLabel = createTempNodeIfDoesNotExist(toId);

            
            
            EdgeLine line = new EdgeLine();
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
            EdgeLine existingLine = edges.putIfAbsent(key, line);
            Validate.isTrue(existingLine == null, "Edge %s -> %s cannot be created because it already exists", fromId, toId);



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
        int color = styleEdge.getColor();
        double width = styleEdge.getWidth();

        return () -> {
            ImmutablePair<String, String> key = new ImmutablePair<>(fromId, toId);
            EdgeLine line = edges.get(key);
            Validate.isTrue(line != null, "Edge %s -> %s cannot be styled because it doesn't exist", fromId, toId);

            line.setColor(color);
            line.setWidth(width);
        };
    }
    
    private Runnable generateRemoveEdgeCode(Object msg) {
        Validate.notNull(msg);
        RemoveEdge removeEdge = (RemoveEdge) msg;
        
        String fromId = removeEdge.getFromId();
        String toId = removeEdge.getToId();

        return () -> {
            NodeLabel fromLabel = nodes.get(fromId);
            NodeLabel toLabel = nodes.get(toId);

            Validate.isTrue(fromLabel != null, // don't check for temporary node here, we can call removeedge with from set to a tempnode
                    "Edge %s -> %s cannot be removed because start node does not exist", fromId, toId);
            Validate.isTrue(toLabel != null, // don't check for temporary node here, we can call removeedge with to set to a temp node
                    "Edge %s -> %s cannot be removed because end node does not exist", fromId, toId);

            
            
            ImmutablePair<String, String> key = new ImmutablePair<>(fromId, toId);
            EdgeLine line = edges.remove(key);
            Validate.isTrue(line != null, "Edge %s -> %s cannot be removed because it doesn't exist", fromId, toId);
            
            
            
            anchors.removeMapping(fromLabel, line);
            anchors.removeMapping(toLabel, line);
            
            deleteTempNodeIfOrphaned(fromId);
            deleteTempNodeIfOrphaned(toId);

            graph.getChildren().remove(line);
        };
    }

    private NodeLabel createTempNodeIfDoesNotExist(String id) {
        NodeLabel label = nodes.get(id);
        if (label != null) {
            return label;
        }
        
        label = new NodeLabel(id);
        label.layoutXProperty().set(0);
        label.layoutYProperty().set(0);
        label.setTemporary(true);
        nodes.put(id, label);
        graph.getChildren().add(label);
        
        return label;
    }


    @SuppressWarnings("unchecked")
    private void deleteTempNodeIfOrphaned(String id) {
        NodeLabel label = nodes.get(id);
        // if label exists and it isn't temporary, we never want to implicitly delete it, so return right away
        if (label == null || !label.isTemporary()) {
            return;
        }

        // at this point node exists and is temp, check to see if it has no more anchors (nothing else connects to/from it)... if so, remove
        Collection<Line> lines = (Collection<Line>) anchors.get(label);
        if (lines == null || lines.isEmpty()) {
            nodes.remove(id);
            anchors.remove(label); // just in case
            graph.getChildren().remove(label);
        }
    }
}
