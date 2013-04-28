package com.offbynull.peernetic.chord;

import com.offbynull.peernetic.p2ptools.identification.BitLimitedId;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedPointer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class Chord {

    private ChordState chordState;
    private Lock chordStateLock;
    private ChordServer chordServer;
    private ChordClient chordClient;
    private State state;
    NEED A TIMER HERE TO CALL STABILIZE EVERY ONCE AND A WHILE;
    NEED A TIMER HERE TO ITERATE FIX FINGER;

    public Chord(BitLimitedId id, int port) throws IOException {
        if (id == null) {
            throw new NullPointerException();
        }
        
        
        InetAddress address = InetAddress.getLocalHost();
        BitLimitedPointer base = new BitLimitedPointer(id,
                new InetSocketAddress(address, port));
        chordState = new ChordState(base);
                
        
        chordStateLock = new ReentrantLock();
        chordServer = new ChordServer(address, port, chordState, chordStateLock);
        chordClient = new ChordClient(chordState, chordStateLock);
    }

    public void start() {
        if (state != State.CREATED) {
            throw new IllegalStateException();
        }
        chordServer.start();
        state = State.STARTED;
    }

    public void start(InetSocketAddress bootstrap) {
        if (state != State.CREATED) {
            throw new IllegalStateException();
        }
        chordServer.start();
        chordClient.join(bootstrap);
        state = State.STARTED;
    }
    
    public void stop() throws InterruptedException {
        if (state != State.STARTED) {
            throw new IllegalStateException();
        }
        chordServer.stop();
        state = State.STOPPED;
    }

    private enum State {
        CREATED,
        STARTED,
        STOPPED
    }
}
