package com.offbynull.peernetic.chord;

import com.offbynull.peernetic.chord.UpdateState.FingerUpdateInstruction;
import com.offbynull.peernetic.chord.UpdateState.SuccessorStabilizeInstruction;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedId;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedPointer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class Chord {

    private ChordState chordState;
    private Lock chordStateLock;
    private ChordServer chordServer;
    private ChordClient chordClient;
    private State state;
    private Timer stabilizeTimer;
    private Timer fixFingerTimer;

    public Chord(BitLimitedId id, int port) throws IOException {
        if (id == null) {
            throw new NullPointerException();
        }
        
        stabilizeTimer = new Timer();
        fixFingerTimer = new Timer();
        
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
        
        int fingerCount = chordState.getBitCount();
        for (int i = 0; i < fingerCount; i++) {
            chordClient.fixFinger(bootstrap, i);
        }
        
        chordClient.join(bootstrap);
        
        state = State.STARTED;
    }
    
    public void stop() throws InterruptedException {
        if (state != State.STARTED) {
            throw new IllegalStateException();
        }
        
        stabilizeTimer.cancel();
        fixFingerTimer.cancel();
        chordServer.stop();
        state = State.STOPPED;
    }

    private final class FixFingerTimerTask extends TimerTask {

        @Override
        public void run() {
            FingerUpdateInstruction inst =
                    chordState.getNextFingerUpdate();
            
            chordClient.fixFinger(inst.getFinger());
            
            long delay = inst.getWaitDuration();
            
            fixFingerTimer.schedule(this, delay);
        }
    }
    
    private final class StabilizeTimerTask extends TimerTask {

        @Override
        public void run() {
            SuccessorStabilizeInstruction inst =
                    chordState.getNextStabilizeUpdate();
            
            chordClient.stabilize();
            
            long delay = inst.getWaitDuration();
            
            stabilizeTimer.schedule(this, delay);
        }
    }
    
    private enum State {
        CREATED,
        STARTED,
        STOPPED
    }
}
