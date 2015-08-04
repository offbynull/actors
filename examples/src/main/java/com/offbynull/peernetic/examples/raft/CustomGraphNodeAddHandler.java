package com.offbynull.peernetic.examples.raft;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.visualizer.gateways.graph.GraphNodeAddHandler;
import com.offbynull.peernetic.visualizer.gateways.graph.NodeProperties;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.Validate;

final class CustomGraphNodeAddHandler implements GraphNodeAddHandler {

    private double radius;

    public CustomGraphNodeAddHandler(int maxNodes) {
        Validate.isTrue(maxNodes > 0);
        radius = Math.log(maxNodes * 100) * 50.0;
    }
    
    @Override
    public NodeProperties nodeAdded(Address graphAddress, String id, AddMode addMode, NodeProperties nodeProperties) {
        Validate.notNull(graphAddress);
        Validate.notNull(id);
        Validate.notNull(addMode);
        // nodeProperties may be null
        
        double percentage = hashToPercentage(id);
        Point point = pointOnCircle(radius, percentage);
        
        return new NodeProperties(id, 0xFFFFFF, point.x, point.y);
        
    }
    
    private static double hashToPercentage(String id) {
        Validate.notNull(id);
        
        byte[] data = DigestUtils.md5(id);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dais = new DataInputStream(bais);
        
        int shortVal;
        try {
            shortVal = dais.readUnsignedShort();
        } catch(IOException ioe) {
            // never happens
            throw new IllegalStateException(ioe);
        }
        
        double ret = (double) shortVal / (double) 0xFFFF;
        
        return Math.min(ret, 1.0); // just in case floatingpoint error results in > 1.0.... dependent on platform java is running on?
    }
    
    /**
     * Creates point on a circle.
     * @param radius radius of circle
     * @param percentage 0 to 1 percentage of where point is on circle -- 0.0 indicates that the point is at the top middle
     * @return new point on circle specified by {@code radius} and {@code percentage}
     * @throws IllegalArgumentException if {@code radius} is negative or a special double value (e.g. NaN/infinite/etc..), or if
     * {@code percentage} isn't between {@code 0.0 - 1.0}
     */
    private static Point pointOnCircle(double radius, double percentage) {
        Validate.inclusiveBetween(0.0, Double.MAX_VALUE, radius);
        Validate.inclusiveBetween(0.0, 1.0, percentage);
        double angle = percentage * Math.PI * 2.0;
        angle -= Math.PI / 2.0; // adjust so that percentage 0.0 is at top middle, if not it'ld be at middle right
        
        double y = (Math.sin(angle) * radius) + radius; // NOPMD
        double x = (Math.cos(angle) * radius) + radius; // NOPMD
        
        return new Point((int) x, (int) y);
    }
    
    private static final class Point {
        private final double x;
        private final double y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
