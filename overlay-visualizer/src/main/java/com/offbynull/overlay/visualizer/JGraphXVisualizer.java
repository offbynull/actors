package com.offbynull.overlay.visualizer;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxGraphView;
import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;

public final class JGraphXVisualizer<A> implements Visualizer<A> {

    private JFrame frame;

    private mxGraph graph;
    private mxGraphComponent component;
    private BidiMap<A, Object> nodeLookupMap;
    private MultiMap<ImmutablePair<A, A>, Object> connToEdgeLookupMap;
    private Map<Object, ImmutablePair<A,A>> edgeToConnLookupMap;
    private NodePlacer<A> placer;
    private AtomicReference<VisualizerEventListener> listener = new AtomicReference<>();

    public JGraphXVisualizer() {
        this(new RandomLocationNodePlacer<A>(400, 400));
    }

    public JGraphXVisualizer(NodePlacer<A> placer) {
        Validate.notNull(placer);

        graph = new mxGraph();
        graph.setCellsEditable(false);
        graph.setAllowDanglingEdges(false);
        graph.setAllowLoops(false);
        graph.setCellsDeletable(false);
        graph.setCellsCloneable(false);
        graph.setCellsDisconnectable(false);
        graph.setDropEnabled(false);
        graph.setSplitEnabled(false);
        graph.setCellsBendable(false);
        graph.setConnectableEdges(false);
        graph.setCellsMovable(false);
        graph.setCellsResizable(false);
        graph.setAutoSizeCells(true);

        component = new mxGraphComponent(graph);
        component.setConnectable(false);

        component.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        component.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        nodeLookupMap = new DualHashBidiMap<>();
        connToEdgeLookupMap = new MultiValueMap<>();
        edgeToConnLookupMap = new HashMap<>();

        this.placer = placer;

        frame = new JFrame("Visualizer");
        frame.setSize(400, 400);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        frame.setContentPane(component);
        
        component.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                zoomFit();
            }
        });
        
        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                VisualizerEventListener veListener = listener.get();
                
                if (veListener != null) {
                    veListener.closed();
                }
            }
        });
    }


    @Override
    public void visualize(final VisualizerEventListener listener) {
        Validate.notNull(listener);
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                
                @Override
                public void run() {
                    JGraphXVisualizer.this.listener.set(listener);
                    frame.setVisible(true);
                }
            });
        } catch (Exception ex) {
            throw new RuntimeException("Visualize failed", ex);
        }
    }

    @Override
    public void visualize() {
        visualize(new VisualizerEventListener() {

            @Override
            public void closed() {
            }
        });
    }

    @Override
    public void moveNode(final A address, final int centerX, final int centerY) {
        Validate.notNull(address);

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                Object vertex = nodeLookupMap.get(address);
                Validate.isTrue(vertex != null);

                mxRectangle rect = graph.getView().getBoundingBox(new Object[]{vertex});
                graph.moveCells(new Object[]{vertex}, centerX - rect.getCenterX(), centerY - rect.getCenterY());

                zoomFit();
            }
        });
    }

    @Override
    public void resizeNode(final A address, final int width, final int height) {
        Validate.notNull(address);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, width);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, height);

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                Object vertex = nodeLookupMap.get(address);
                Validate.isTrue(vertex != null);

                mxGraphView view = graph.getView();
                mxRectangle rect = graph.getBoundingBox(vertex);

                rect.setWidth(width);
                rect.setHeight(height);
                rect.setX(rect.getX() / view.getScale());
                rect.setY(rect.getY() / view.getScale());
                graph.resizeCell(vertex, rect);

                zoomFit();
            }
        });
    }

    @Override
    public void scaleNode(final A address, final double scale) {
        Validate.notNull(address);
        Validate.inclusiveBetween(0.0, Double.POSITIVE_INFINITY, scale);

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                Object vertex = nodeLookupMap.get(address);
                Validate.isTrue(vertex != null);

                mxGraphView view = graph.getView();
                mxRectangle rect = graph.getBoundingBox(vertex);

                rect.setWidth(rect.getWidth() / view.getScale() * scale);
                rect.setHeight(rect.getHeight() / view.getScale() * scale);
                rect.setX(rect.getX() / view.getScale());
                rect.setY(rect.getY() / view.getScale());
                graph.resizeCell(vertex, rect);

                zoomFit();
            }
        });
    }

    @Override
    public void setNodeColor(final A address, final Color color) {
        Validate.notNull(address);
        Validate.notNull(color);
        Validate.isTrue(color.getAlpha() == 255);

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                Object vertex = nodeLookupMap.get(address);
                Validate.isTrue(vertex != null);

                graph.setCellStyle(mxConstants.STYLE_FILLCOLOR + "=" + "#" + String.format("%06x", color.getRGB() & 0x00FFFFFF),
                        new Object[]{vertex});
                nodeLookupMap.put(address, vertex);

                zoomFit();
            }
        });
    }

    @Override
    public void addNode(final A address) {
        Validate.notNull(address);
        Validate.notNull(placer);

        NodePlacementInfo info = placer.placeNode(address);

        addNode(address, info.getCenterX(), info.getCenterY());
        setNodeColor(address, info.getColor());
        scaleNode(address, info.getScale());
    }

    private void addNode(final A address, final int centerX, final int centerY) {
        Validate.notNull(address);
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                Validate.isTrue(!nodeLookupMap.containsKey(address));

                Object parent = graph.getDefaultParent();

                Object vertex = graph.insertVertex(parent, null, address, 0, 0, 1, 1);
                graph.updateCellSize(vertex);
                graph.moveCells(new Object[]{vertex}, 0, 0);

                mxGraphView view = graph.getView();
                mxRectangle rect = graph.getBoundingBox(vertex);

                rect.setWidth(rect.getWidth() / view.getScale());
                rect.setHeight(rect.getHeight() / view.getScale());
                rect.setX(rect.getX() / view.getScale());
                rect.setY(rect.getY() / view.getScale());

                graph.moveCells(new Object[]{vertex}, centerX - rect.getCenterX(), centerY - rect.getCenterY());

                graph.getView().validate();

                nodeLookupMap.put(address, vertex);

                zoomFit();
            }
        });
    }

    @Override
    public void removeNode(final A address) {
        Validate.notNull(address);

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                Validate.isTrue(nodeLookupMap.containsKey(address));

                Object vertex = nodeLookupMap.remove(address);
                Object[] edges = graph.getEdges(vertex);

                for (Object edge : edges) {
                    ImmutablePair<A, A> conn = edgeToConnLookupMap.get(edge);
                    connToEdgeLookupMap.removeMapping(conn, edge);
                }

                graph.removeCells(new Object[]{vertex});

                zoomFit();
            }
        });
    }

    @Override
    public void addConnection(final A from, final A to) {
        Validate.notNull(from);
        Validate.notNull(to);

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                Object parent = graph.getDefaultParent();

                Object fromVertex = nodeLookupMap.get(from);
                Object toVertex = nodeLookupMap.get(to);
                ImmutablePair<A, A> conn = new ImmutablePair<>(from, to);
                Validate.isTrue(nodeLookupMap.containsKey(from), "Connection source doesn't exist");
                Validate.isTrue(nodeLookupMap.containsKey(to), "Connection destination doesn't exist");

                Object edge = graph.insertEdge(parent, null, null, fromVertex, toVertex);
                connToEdgeLookupMap.put(conn, edge);
                edgeToConnLookupMap.put(edge, conn);

                zoomFit();
            }
        });
    }

    @Override
    public void removeConnection(final A from, final A to) {

        Validate.notNull(from);
        Validate.notNull(to);

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                Object fromVertex = nodeLookupMap.get(from);
                Object toVertex = nodeLookupMap.get(to);
                ImmutablePair<A, A> conn = new ImmutablePair<>(from, to);
                Validate.isTrue(nodeLookupMap.containsKey(from), "Connection source doesn't exist");
                Validate.isTrue(nodeLookupMap.containsKey(to), "Connection destination doesn't exist");
                Validate.isTrue(connToEdgeLookupMap.containsKey(conn), "Connection doesn't exists");

                if (fromVertex == null || toVertex == null) {
                    return;
                }
                
                Collection<Object> edges = (Collection<Object>) connToEdgeLookupMap.get(conn);
                Object edge = edges.iterator().next();
                

                connToEdgeLookupMap.removeMapping(conn, edge);
                edgeToConnLookupMap.remove(edge);

                graph.removeCells(new Object[]{edge});

                zoomFit();
            }
        });
    }

    private void zoomFit() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                double compWidth = component.getWidth();
                double compHeight = component.getHeight();
                double compLen = Math.min(compWidth, compHeight);

                mxGraphView view = graph.getView();
                double oldScale = view.getScale();
                mxPoint oldTranslate = view.getTranslate();

                mxRectangle graphBounds = view.getGraphBounds();
                double graphX = (graphBounds.getX()) / oldScale - oldTranslate.getX();
                double graphY = (graphBounds.getY()) / oldScale - oldTranslate.getY();
                double graphWidth = (graphBounds.getWidth()) / oldScale;
                double graphHeight = (graphBounds.getHeight()) / oldScale;
                double graphEndX = graphX + graphWidth;
                double graphEndY = graphY + graphHeight;

                double viewLen = Math.max(graphEndX, graphEndY);

                if (Double.isInfinite(viewLen) || Double.isNaN(viewLen) || viewLen <= 0.0) {
                    view.setScale(1.0);
                } else {
                    double newScale = compLen / viewLen;
                    view.scaleAndTranslate(newScale, -graphX, -graphY);
                }
            }
        });
    }
}
