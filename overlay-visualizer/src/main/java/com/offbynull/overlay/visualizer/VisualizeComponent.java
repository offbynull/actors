package com.offbynull.overlay.visualizer;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxGraphView;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.JComponent;
import javax.swing.ScrollPaneConstants;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;

public final class VisualizeComponent<A> extends JComponent {

    private mxGraph graph;
    private mxGraphComponent component;
    private BidiMap<A, Object> nodeLookupMap;
    private BidiMap<ImmutablePair<A, A>, Object> connLookupMap;
    private Lock updateLock;
    private NodePlacer<A> placer;

    public VisualizeComponent() {
        this(new RandomLocationNodePlacer<A>(1000, 1000));
    }
    
    public VisualizeComponent(NodePlacer<A> placer) {
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
        

        super.setLayout(new BorderLayout());
        super.add(component, BorderLayout.CENTER);
        
        nodeLookupMap = new DualHashBidiMap<>();
        connLookupMap = new DualHashBidiMap<>();
        updateLock = new ReentrantLock();
        
        this.placer = placer;
        
        super.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                zoomFit();
            }
        });
    }

    public void moveNode(A address, int centerX, int centerY) {
        Validate.notNull(address);
        
        updateLock.lock();
        try {
            Object vertex = nodeLookupMap.get(address);
            Validate.isTrue(vertex != null);
            
            graph.getModel().beginUpdate();
            try {
                mxRectangle rect = graph.getView().getBoundingBox(new Object[] { vertex });
                graph.moveCells(new Object[] { vertex }, centerX - rect.getCenterX(), centerY - rect.getCenterY());
            } finally {
                graph.getModel().endUpdate();
            }
        } finally {
            updateLock.unlock();
        }
    }

    public void resizeNode(A address, int width, int height) {
        Validate.notNull(address);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, width);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, height);
        
        updateLock.lock();
        try {
            Object vertex = nodeLookupMap.get(address);
            Validate.isTrue(vertex != null);
            
            graph.getModel().beginUpdate();
            try {
                mxRectangle rect = new mxRectangle(0.0, 0.0, width, height);
                graph.resizeCell(vertex, rect);
            } finally {
                graph.getModel().endUpdate();
            }
        } finally {
            updateLock.unlock();
        }
    }

    public void scaleNode(A address, double scale) {
        Validate.notNull(address);
        Validate.inclusiveBetween(0.0, Double.POSITIVE_INFINITY, scale);
        
        updateLock.lock();
        try {
            Object vertex = nodeLookupMap.get(address);
            Validate.isTrue(vertex != null);
            
            graph.getModel().beginUpdate();
            try {
                mxRectangle rect = graph.getView().getBoundingBox(new Object[] { vertex });
                
                rect.setWidth(rect.getWidth() * scale);
                rect.setHeight(rect.getHeight() * scale);
                graph.resizeCell(vertex, rect);
            } finally {
                graph.getModel().endUpdate();
            }
        } finally {
            updateLock.unlock();
        }
    }

    public void setNodeColor(A address, Color color) {
        Validate.notNull(address);
        Validate.notNull(color);
        Validate.isTrue(color.getAlpha() == 255);
        
        updateLock.lock();
        try {
            Object vertex = nodeLookupMap.get(address);
            Validate.isTrue(vertex != null);
            
            graph.getModel().beginUpdate();
            try {
                graph.setCellStyle(mxConstants.STYLE_FILLCOLOR + "=" + "#" + String.format("%06x", color.getRGB() & 0x00FFFFFF),
                        new Object[] { vertex });
                nodeLookupMap.put(address, vertex);
            } finally {
                graph.getModel().endUpdate();
            }
        } finally {
            updateLock.unlock();
        }
    }
    
    public void addNode(A address) {
        Validate.notNull(address);
        Validate.notNull(placer);
                
        NodePlacementInfo info = placer.placeNode(address);
        
        addNode(address, info.getCenterX(), info.getCenterY());
        setNodeColor(address, info.getColor());
        scaleNode(address, info.getScale());
    }
    
    public void addNode(A address, int centerX, int centerY) {
        Validate.notNull(address);
        
        updateLock.lock();
        try {
            Validate.isTrue(!nodeLookupMap.containsKey(address));
            
            Object parent = graph.getDefaultParent();
            
            Object vertex;
            graph.getModel().beginUpdate();
            try {
                vertex = graph.insertVertex(parent, null, address, 0, 0, 1, 1);
                graph.updateCellSize(vertex);
                graph.moveCells(new Object[] { vertex }, 0, 0);
            } finally {
                graph.getModel().endUpdate();
            }

            graph.getModel().beginUpdate();
            try {
                mxRectangle rect = graph.getView().getBoundingBox(new Object[] { vertex });
                graph.moveCells(new Object[] { vertex }, centerX - rect.getCenterX(), centerY - rect.getCenterY());
                nodeLookupMap.put(address, vertex);
            } finally {
                graph.getModel().endUpdate();
            }
        } finally {
            updateLock.unlock();
        }
    }

    public void removeNode(A address) {
        Validate.notNull(address);
        
        updateLock.lock();
        try {
            Validate.isTrue(nodeLookupMap.containsKey(address));
            
            graph.getModel().beginUpdate();
            try {
                Object vertex = nodeLookupMap.remove(address);
                Object[] edges = graph.getEdges(vertex);
                
                for (Object edge : edges) {
                    connLookupMap.removeValue(edge);
                }
                
                graph.removeCells(new Object[] { vertex });
            } finally {
                graph.getModel().endUpdate();
            }
        } finally {
            updateLock.unlock();
        }
    }
    
    public void addConnection(A from, A to) {
        Validate.notNull(from);
        Validate.notNull(to);
        
        updateLock.lock();
        try {
            Object parent = graph.getDefaultParent();
            
            graph.getModel().beginUpdate();
            try {
                Object fromVertex = nodeLookupMap.get(from);
                Object toVertex = nodeLookupMap.get(to);
                ImmutablePair<A, A> key = new ImmutablePair<>(from, to);
                Validate.isTrue(nodeLookupMap.containsKey(from) && nodeLookupMap.containsKey(to) && !connLookupMap.containsKey(key));
                
                Object edge = graph.insertEdge(parent, null, null, fromVertex, toVertex);
                connLookupMap.put(key, edge);
            } finally {
                graph.getModel().endUpdate();
            }
        } finally {
            updateLock.unlock();
        }
    }

    public void removeConnection(A from, A to) {
        Validate.notNull(from);
        Validate.notNull(to);

        updateLock.lock();
        try {
            graph.getModel().beginUpdate();
            try {
                Object fromVertex = nodeLookupMap.get(from);
                Object toVertex = nodeLookupMap.get(to);
                ImmutablePair<A, A> key = new ImmutablePair<>(from, to);
                Validate.isTrue(nodeLookupMap.containsKey(from) && nodeLookupMap.containsKey(to) && !connLookupMap.containsKey(key));
                
                if (fromVertex == null || toVertex == null) {
                    return;
                }
                
                Object edge = connLookupMap.get(key);
                
                graph.removeCells(new Object[] { edge });
            } finally {
                graph.getModel().endUpdate();
            }
        } finally {
            updateLock.unlock();
        }
    }
    
    private void zoomFit() {
        updateLock.lock();
        try {
            mxGraphView view = graph.getView();
            double compLen = Math.min(component.getWidth(), component.getHeight());
            double viewLen = view.getGraphBounds().getWidth();
            view.setScale(compLen/viewLen * view.getScale());
        } finally {
            updateLock.unlock();
        }
    }
    
    @Override
    public void setLayout(LayoutManager mgr) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(Component comp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(Component comp, Object constraints, int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(Component comp, Object constraints) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Component add(Component comp, int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Component add(String name, Component comp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Component add(Component comp) {
        throw new UnsupportedOperationException();
    }

}
