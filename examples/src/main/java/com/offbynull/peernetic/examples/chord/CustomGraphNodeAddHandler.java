package com.offbynull.peernetic.examples.chord;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.visualizer.gateways.graph.GraphNodeAddHandler;
import com.offbynull.peernetic.visualizer.gateways.graph.NodeProperties;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Scanner;
import org.apache.commons.lang3.Validate;

final class CustomGraphNodeAddHandler implements GraphNodeAddHandler {

    private final double radius;
    private final BigDecimal limitDec;

    public CustomGraphNodeAddHandler(int maxNodes) {
        Validate.isTrue(maxNodes > 0);
        this.radius = Math.max(300, maxNodes * 4);
        this.limitDec = new BigDecimal("" + maxNodes);
    }
    
    @Override
    public NodeProperties nodeAdded(Address graphAddress, String id, AddMode addMode, NodeProperties nodeProperties) {
        Validate.notNull(graphAddress);
        Validate.notNull(id);
        Validate.notNull(addMode);
        // nodeProperties may be null
        
        double percentage = derivePercentageFromId(id);
        Point point = pointOnCircle(radius, percentage);
        
        return new NodeProperties(id, 0xFF0000, point.x, point.y);
        
    }
    
    private double derivePercentageFromId(String id) {
        Scanner scanner = new Scanner(id);
        scanner.useDelimiter("[^\\d]+");  // everything other than digit and dot is skipped
        
        BigDecimal idDec = new BigDecimal(scanner.nextBigInteger());
        double percentage = idDec.divide(limitDec, 10, RoundingMode.FLOOR).doubleValue();
        
        return percentage;
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
