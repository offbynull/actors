package com.offbynull.peernetic.debug.testnetwork;

public final class SimpleLineFactory implements LineFactory {

    public static final SimpleLineFactory INSTANCE = new SimpleLineFactory();
    
    @Override
    public <A> Line<A> createLine() {
        return new SimpleLine<>();
    }
    
}
