package com.offbynull.peernetic.chord;

import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.StreamServer;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedId;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedPointer;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.locks.Lock;

public final class ChordServer {
    private ChordState state;
    private Lock stateLock;
    private StreamServer rpcServer;

    public ChordServer(InetAddress bindAddress, int bindPort, ChordState state,
            Lock stateLock) throws IOException {
        if (state == null || stateLock == null) {
            throw new NullPointerException();
        }
        
        this.state = state;
        this.stateLock = stateLock;

        ChordServerInterface impl = new Implementation();
        JsonRpcServer jsonRpcServer = new JsonRpcServer(impl,
                ChordServerInterface.class);
        rpcServer = new StreamServer(jsonRpcServer, 50, bindPort, 100,
                bindAddress);
    }
    
    public void start() {
        rpcServer.start();
    }

    public void stop() throws InterruptedException {
        rpcServer.stop();
    }

    private final class Implementation implements ChordServerInterface {
        @Override
        public void notify(BitLimitedPointer predecessor) {
            stateLock.lock();
            try {
                state.setPredecessor(predecessor);
            } finally {
                stateLock.unlock();
            }
        }

        @Override
        public BitLimitedPointer getPredecessor() {
            stateLock.lock();
            try {
                return state.getPredecessor();
            } finally {
                stateLock.unlock();
            }
        }

        @Override
        public RouteResult route(BitLimitedId id) {
            stateLock.lock();
            try {
                return state.route(id);
            } finally {
                stateLock.unlock();
            }
        }

        @Override
        public BitLimitedId getId() {
            stateLock.lock();
            try {
                return state.getBaseId();
            } finally {
                stateLock.unlock();
            }
        }

        @Override
        public long ping() {
            return System.currentTimeMillis();
        }

        @Override
        public List<BitLimitedPointer> getSuccesorList() {
            stateLock.lock();
            try {
                return state.dumpSuccessorTable();
            } finally {
                stateLock.unlock();
            }
        }
    }
}
