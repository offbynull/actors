package com.offbynull.overlay.visualizer;

import java.io.Closeable;

public interface Recorder<A> extends Closeable {
    public void recordStep(String output, Command<A> ... commands);
}
