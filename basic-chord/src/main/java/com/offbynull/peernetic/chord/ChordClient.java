package com.offbynull.peernetic.chord;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import static com.offbynull.peernetic.chord.RouteResult.ResultType.CLOSEST_PREDECESSOR;
import static com.offbynull.peernetic.chord.RouteResult.ResultType.FOUND;
import static com.offbynull.peernetic.chord.RouteResult.ResultType.SELF;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedId;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedPointer;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.locks.Lock;

public final class ChordClient {
    private ChordState state;
    private Lock stateLock;

    public ChordClient(ChordState state, Lock stateLock) {
        if (state == null || stateLock == null) {
            throw new NullPointerException();
        }
        
        this.state = state;
        this.stateLock = stateLock;
    }

    public void join(InetSocketAddress address) {
        ChordServerInterface rpcService = getRpcService(address);
        BitLimitedId id = rpcService.getId();
        BitLimitedPointer bootstrap = new BitLimitedPointer(id, address);

        BitLimitedId baseId;
        
        stateLock.lock();
        try {
            state.putFinger(bootstrap);
            baseId = state.getBaseId();
        } finally {
            stateLock.unlock();
        }
        
        BitLimitedPointer successor = null;
        List<BitLimitedPointer> successorTable = null;
        try {
            successor = findNode(baseId);
            successorTable = getRpcService(successor.getAddress())
                    .getSuccesorList();
        } catch (RuntimeException re) {
            stateLock.lock();
            try {
                state.removeFinger(bootstrap);
            } finally {
                stateLock.unlock();
            }
            throw re;
        }
        
        if (successor.getId().equals(baseId)) {
            throw new IllegalStateException();
        }
        
        stateLock.lock();
        try {
            state.setSuccessor(successor, successorTable);
        } finally {
            stateLock.unlock();
        }
    }

    public void stabilize() {
        BitLimitedPointer successor;
        BitLimitedPointer base;
        
        stateLock.lock();
        try {
            successor = state.getSuccessor();
            base = state.getBase();
        } finally {
            stateLock.unlock();
        }
        
        if (successor == null) {
            return;
        }
        
        BitLimitedId baseId = base.getId();
        
        
        InetSocketAddress successorAddress = successor.getAddress();
        BitLimitedId successorId = successor.getId();
        ChordServerInterface successorRpcService =
                getRpcService(successorAddress);
        
        BitLimitedPointer successorsPred = successorRpcService.getPredecessor();
        BitLimitedId successorsPredId = successorsPred.getId();
        InetSocketAddress successorPredAddress = successorsPred.getAddress();
        ChordServerInterface successorPredRpcService =
                getRpcService(successorPredAddress);
        
        BitLimitedPointer newSuccessor = successor;
        if (successorsPredId.isWithin(baseId, false, successorId, false)) {
            List<BitLimitedPointer> successorPredSuccessors =
                    successorPredRpcService.getSuccesorList();
            
            stateLock.lock();
            try {
                state.setSuccessor(successorsPred, successorPredSuccessors);
                newSuccessor = state.getSuccessor();
            } finally {
                stateLock.unlock();
            }
        }
        
        ChordServerInterface newSuccessorRpcService =
                getRpcService(newSuccessor.getAddress());
        newSuccessorRpcService.notify(base);
    }
    
    public BitLimitedPointer findNode(InetSocketAddress bootstrap,
            BitLimitedId id) {
        InetSocketAddress address = bootstrap;
        while (true) {
            ChordServerInterface rpcService = getRpcService(address);
            RouteResult res = rpcService.route(id);
            
            switch (res.getResultType()) {
                case CLOSEST_PREDECESSOR:
                    break;
                case SELF:
                case FOUND:
                    return res.getPointer();
            }
        }
    }
    
    public BitLimitedPointer findNode(BitLimitedId id) {
        RouteResult res;
        
        stateLock.lock();
        try {
            res = state.route(id);
        } finally {
            stateLock.unlock();
        }

        switch (res.getResultType()) {
            case CLOSEST_PREDECESSOR:
                break;
            case SELF:
            case FOUND:
                return res.getPointer();
        }
        
        return findNode(res.getPointer().getAddress(), id);
    }
    
    public void checkPredecessor() {
        BitLimitedPointer pred;
        
        stateLock.lock();
        try {
            pred = state.getPredecessor();
        } finally {
            stateLock.unlock();
        }
        
        ChordServerInterface rpcService = getRpcService(pred.getAddress());
        
        try {
            rpcService.ping();
        } catch (RuntimeException re) {
            // node is dead, remove predecessor
            stateLock.lock();
            try {
                // check to make sure predecessor did not change before removing
                BitLimitedPointer newPred = state.getPredecessor();
                if (pred.equals(newPred)) {
                    state.removePredecessor();
                }
            } finally {
                stateLock.unlock();
            }
        }
    }

    public void checkSuccessor() {
        BitLimitedPointer successor;
        BitLimitedPointer base;
        
        
        stateLock.lock();
        try {
            base = state.getBase();
            successor = state.getSuccessor();
        } finally {
            stateLock.unlock();
        }
        
        if (base.equals(successor)) {
            return;
        }
        
        ChordServerInterface rpcService = getRpcService(successor.getAddress());
        
        try {
            rpcService.ping();
        } catch (RuntimeException re) {
            // node is dead, remove predecessor
            stateLock.lock();
            try {
                // check to make sure successor did not change before shifting
                BitLimitedPointer newSuccessor = state.getSuccessor();
                if (successor.equals(newSuccessor)) {
                    state.shiftSuccessor();
                }
            } finally {
                stateLock.unlock();
            }
        }
    }
    
    public void fixFinger(int idx) {
        // 1 handled by checkSuccessor
        if (idx <= 0) {
            throw new IllegalArgumentException();
        }
        
        BitLimitedId searchId;
        
        stateLock.lock();
        try {
            searchId = state.getExpectedFingerId(idx);
        } finally {
            stateLock.unlock();
        }
        
        BitLimitedPointer found = findNode(searchId);
        
        stateLock.lock();
        try {
            state.putFinger(found);
        } finally {
            stateLock.unlock();
        }
    }
    
    public void fixFinger(InetSocketAddress bootstrap, int idx) {
        // 1 handled by checkSuccessor
        if (idx <= 0) {
            throw new IllegalArgumentException();
        }
        
        BitLimitedId searchId;
        
        stateLock.lock();
        try {
            searchId = state.getExpectedFingerId(idx);
        } finally {
            stateLock.unlock();
        }
        
        BitLimitedPointer found = findNode(bootstrap, searchId);
        
        stateLock.lock();
        try {
            state.putFinger(found);
        } finally {
            stateLock.unlock();
        }
    }

    private ChordServerInterface getRpcService(InetSocketAddress address)
            throws IllegalArgumentException {
        
        URL url;
        try {
            url = new URL("http://" + address.getHostString() + ":"
                    + address.getPort());
        } catch (MalformedURLException mue) {
            throw new IllegalArgumentException(mue);
        }
        JsonRpcHttpClient client = new JsonRpcHttpClient(url);
        ChordServerInterface rpcService = ProxyUtil.createClientProxy(
                getClass().getClassLoader(),
                ChordServerInterface.class,
                client);
        return rpcService;
    }
}
