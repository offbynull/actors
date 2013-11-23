package com.offbynull.rpc;

import com.offbynull.rpc.invoke.Invoker;
import com.offbynull.rpc.invoke.InvokerCallback;
import com.offbynull.rpc.transport.IncomingMessage;
import com.offbynull.rpc.transport.IncomingMessageListener;
import com.offbynull.rpc.transport.IncomingMessageResponseHandler;
import com.offbynull.rpc.transport.OutgoingResponse;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.lang3.Validate;

final class ServiceRouter<A> {
    private static final int LISTER_SERVICE_ID = 0;
    
    private ReadWriteLock lock;
    
    private SortedSet<Integer> listedServiceSet;
    private Map<Integer, String> serviceNameMap;
    private ListerService listerService;
    
    private ServerMessageToInvokeCallback messageListener;
    
    private Map<Integer, ServiceEntry> invokerMap;
    
    private ExecutorService executorService;
    private Map<? extends Object, ? extends Object> extraInvokeInfo;
    private Rpc rpc;
    

    public ServiceRouter(ExecutorService executorService, Rpc<A> rpc, Map<? extends Object, ? extends Object> extraInvokeDataMap) {
        Validate.notNull(executorService);
        Validate.notNull(rpc);
        Validate.notNull(extraInvokeDataMap);
    
        this.executorService = executorService;
        this.rpc = rpc;
        this.extraInvokeInfo = extraInvokeDataMap;
        
        
        
        lock = new ReentrantReadWriteLock();

        
        invokerMap = new HashMap<>();

        listedServiceSet = new TreeSet<>();
        serviceNameMap = new HashMap<>();

        listerService = new ListerServiceImplementation(lock, Collections.unmodifiableSortedSet(listedServiceSet),
                Collections.unmodifiableMap(serviceNameMap));

        listedServiceSet.add(LISTER_SERVICE_ID);
        serviceNameMap.put(LISTER_SERVICE_ID, ListerServiceImplementation.class.getName());

        invokerMap.put(LISTER_SERVICE_ID, new ServiceEntry(LISTER_SERVICE_ID, listerService));

        messageListener = new ServerMessageToInvokeCallback();
    }
    
    public void addService(int id, Object object) {
        Validate.isTrue(id != 0, "Reserved id");
        Validate.notNull(object);
        
        lock.writeLock().lock();
        try {
            Validate.isTrue(!invokerMap.containsKey(id));
            invokerMap.put(id, new ServiceEntry(id, object));
            serviceNameMap.put(id, object.getClass().getName());
            listedServiceSet.add(id);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void removeService(int id) {
        Validate.isTrue(id != 0, "Reserved id");
        
        lock.writeLock().lock();
        try {
            invokerMap.remove(id);
            serviceNameMap.remove(id);
            listedServiceSet.remove(id);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    IncomingMessageListener<A> getIncomingMessageListener() {
        return messageListener;
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
    
    private final class ServerMessageToInvokeCallback implements IncomingMessageListener<A> {

        @Override
        public void messageArrived(IncomingMessage<A> message, IncomingMessageResponseHandler responseCallback) {
            ByteBuffer buffer = message.getData();
            A from = message.getFrom();
            
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
            
            Map<Object, Object> invokeInfo = new HashMap<>();
            invokeInfo.put(RpcInvokeKeys.FROM_ADDRESS, from);
            invokeInfo.put(RpcInvokeKeys.RPC, rpc);
            invokeInfo.putAll(extraInvokeInfo);
            
            Invoker invoker = serviceEntry.getInvoker();
            invoker.invoke(dataWithoutId, new InvokeResponseToServerResponseCallback(responseCallback), invokeInfo);
        }
        
    }
    
    private final class InvokeResponseToServerResponseCallback implements InvokerCallback {
        
        private IncomingMessageResponseHandler serverCallback;

        public InvokeResponseToServerResponseCallback(IncomingMessageResponseHandler serverCallback) {
            Validate.notNull(serverCallback);
            
            this.serverCallback = serverCallback;
        }
        
        @Override
        public void invokationFailed(Throwable t) {
            serverCallback.terminate();
        }

        @Override
        public void invokationFinised(byte[] data) {
            OutgoingResponse response = new OutgoingResponse(data);
            serverCallback.responseReady(response);
        }
    }
}
