package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.visualizer.gateways.graph.GraphNodeAddHandler;
import com.offbynull.peernetic.visualizer.gateways.graph.NodeProperties;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.Validate;

final class CustomGraphNodeAddHandler implements GraphNodeAddHandler {

    private final int maxX;
    private final int maxY;

    public CustomGraphNodeAddHandler(int maxX, int maxY) {
        Validate.isTrue(maxX > 0);
        Validate.isTrue(maxY > 0);
        this.maxX = maxX;
        this.maxY = maxY;
    }
    
    @Override
    public NodeProperties nodeAdded(Address graphAddress, String id, AddMode addMode, NodeProperties nodeProperties) {
        Validate.notNull(graphAddress);
        Validate.notNull(id);
        Validate.notNull(addMode);
        // nodeProperties may be null
        
        byte[] data = DigestUtils.md5(id);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dais = new DataInputStream(bais);
        
        int rand1;
        int rand2;
        try {
            rand1 = dais.readInt();
            rand2 = dais.readInt();
        } catch(IOException ioe) {
            // never happens
            throw new IllegalStateException(ioe);
        }
        
        rand1 &= 0x7F000000; // remove negative bit
        rand2 &= 0x7F000000; // remove negative bit
        
        int x = rand1 % maxX;
        int y = rand2 % maxY;
        
        return new NodeProperties(id, 0xFFFFFF, (double) x, (double) y);
        
    }
    
}
