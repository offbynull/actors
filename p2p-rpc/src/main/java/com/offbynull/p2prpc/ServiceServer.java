package com.offbynull.p2prpc;

import com.offbynull.p2prpc.invoke.Invoker;
import com.offbynull.p2prpc.invoke.InvokerCallback;
import com.offbynull.p2prpc.session.Server;
import com.offbynull.p2prpc.session.ServerMessageCallback;
import com.offbynull.p2prpc.session.ServerResponseCallback;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.lang3.Validate;

public final class ServiceServer<A> {
    private static final int LISTER_SERVICE_ID = 0;
    
    private ReadWriteLock lock;
    
    private SortedSet<Integer> listedServices;
    private ListerService listerService;
    
    private Server<A> server;
    private Map<Integer, ServiceEntry> invokerMap;
    private ExecutorService executorService;
    
    private State state = State.UNKNOWN;

    public ServiceServer(Server<A> server, ExecutorService executorService) {
        Validate.notNull(server);
        Validate.notNull(executorService);
        
        this.server = server;
        this.executorService = executorService;
        lock = new ReentrantReadWriteLock();
    }
    
    public void start() throws IOException {
        lock.writeLock().lock();
        
        try {
            Validate.validState(state == State.UNKNOWN);
            
            invokerMap = new HashMap<>();
            
            listedServices = new TreeSet<>();
            listedServices.add(LISTER_SERVICE_ID);
            listerService = new ListerServiceImplementation(lock, listedServices);
            invokerMap.put(LISTER_SERVICE_ID, new ServiceEntry(LISTER_SERVICE_ID, listerService));
            
            server.start(new ServerMessageToInvokeCallback());
        } finally {
            state = State.STARTED;
            lock.writeLock().unlock();
        }
    }
    
    
    public void stop() throws IOException {
        lock.writeLock().lock();
        
        try {
            Validate.validState(state == State.STARTED);
            server.stop();
        } finally {
            state = State.STOPPED;
            lock.writeLock().unlock();
        }        
    }
    
    public void addService(int id, Object object) {
        Validate.isTrue(id != 0, "Reserved id");
        
        lock.writeLock().lock();
        try {
            Validate.validState(state == State.STARTED);
            Validate.isTrue(!invokerMap.containsKey(id));
            invokerMap.put(id, new ServiceEntry(id, object));
            listedServices.add(id);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void removeService(int id) {
        Validate.isTrue(id != 0, "Reserved id");
        
        lock.writeLock().lock();
        try {
            Validate.validState(state == State.STARTED);
            invokerMap.remove(id);
            listedServices.remove(id);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private final class ServiceEntry {
        private int id;
        private Invoker invoker;
        private Object object;

        public ServiceEntry(int id, Object object) {
            this.id = id;
            this.object = object;
            invoker = new Invoker(object, executorService);
        }

        public int getId() {
            return id;
        }

        public Invoker getInvoker() {
            return invoker;
        }

        public Object getObject() {
            return object;
        }
    }
    
    private final class ServerMessageToInvokeCallback implements ServerMessageCallback<A> {

        @Override
        public void messageArrived(A from, byte[] data, ServerResponseCallback responseCallback) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            
            int id = buffer.getInt();
            
            ServiceEntry serviceEntry;
            lock.readLock().lock();
            try {
                serviceEntry = invokerMap.get(id);
            } finally {
                lock.readLock().unlock();
            }
            
            if (serviceEntry == null) {
                responseCallback.terminate();
                return;
            }
            
            byte[] dataWithoutId = new byte[buffer.remaining()];
            buffer.get(dataWithoutId);
            
            Invoker invoker = serviceEntry.getInvoker();
            invoker.invoke(dataWithoutId, new InvokeResponseToServerResponseCallback(responseCallback));
        }
        
    }
    
    private final class InvokeResponseToServerResponseCallback implements InvokerCallback {
        
        private ServerResponseCallback serverCallback;

        public InvokeResponseToServerResponseCallback(ServerResponseCallback serverCallback) {
            Validate.notNull(serverCallback);
            
            this.serverCallback = serverCallback;
        }
        
        @Override
        public void invokationFailed(Throwable t) {
            serverCallback.terminate();
        }

        @Override
        public void invokationFinised(byte[] data) {
            serverCallback.responseReady(data);
        }
    }
    
    private enum State {
        UNKNOWN,
        STARTED,
        STOPPED
    }
}
