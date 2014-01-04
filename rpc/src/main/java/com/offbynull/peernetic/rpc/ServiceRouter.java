/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.rpc;

import static com.offbynull.peernetic.rpc.ListerService.SERVICE_ID;
import com.offbynull.peernetic.rpc.invoke.Invoker;
import com.offbynull.peernetic.rpc.invoke.InvokerListener;
import com.offbynull.peernetic.rpc.invoke.invokers.reflection.ReflectionInvoker;
import com.offbynull.peernetic.rpc.transport.IncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.IncomingMessageResponseListener;
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

        listedServiceSet.add(SERVICE_ID);
        serviceNameMap.put(SERVICE_ID, ListerServiceImplementation.class.getName());

        invokerMap.put(SERVICE_ID, new ServiceEntry(SERVICE_ID, listerService));

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
            invoker = new ReflectionInvoker(object, executorService);
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
        public void messageArrived(A from, ByteBuffer message, IncomingMessageResponseListener responseCallback) {
            int id = message.getInt();
            
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
            
            byte[] dataWithoutId = new byte[message.remaining()];
            message.get(dataWithoutId);
            
            Map<Object, Object> invokeInfo = new HashMap<>();
            invokeInfo.put(RpcInvokeKeys.FROM_ADDRESS, from);
            invokeInfo.put(RpcInvokeKeys.RPC, rpc);
            invokeInfo.putAll(extraInvokeInfo);
            
            Invoker invoker = serviceEntry.getInvoker();
            invoker.invoke(dataWithoutId, new InvokeResponseToServerResponseCallback(responseCallback), invokeInfo);
        }
       
    }
    
    private final class InvokeResponseToServerResponseCallback implements InvokerListener {
        
        private IncomingMessageResponseListener serverCallback;

        public InvokeResponseToServerResponseCallback(IncomingMessageResponseListener serverCallback) {
            Validate.notNull(serverCallback);
            
            this.serverCallback = serverCallback;
        }
        
        @Override
        public void invocationFailed(Throwable t) {
            serverCallback.terminate();
        }

        @Override
        public void invocationFinised(byte[] data) {
            serverCallback.responseReady(ByteBuffer.wrap(data));
        }
    }
}
