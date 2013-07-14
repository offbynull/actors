package com.offbynull.p2prpc;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public final class RpcSystem {
//
//    private Map<Object, Server> serverMap;
//
//    public RpcSystem() {
//        serverMap = new HashMap<>();
//    }
//
//    public <T> void startTcpRpcServer(T object, int port)
//            throws InterruptedException {
//        Server server = new Server();
//        server.start(port, object);
//        serverMap.put(object, server);
//    }
//
//    public <T> void stopTcpRpcServer(T object)
//            throws InterruptedException {
//        Server server = serverMap.remove(object);
//        server.stop();
//    }
//
//    public <T> T createTcpClient(Class<T> clz,
//            final InetSocketAddress address) {
//        return  (T) Enhancer.create(clz, new MethodInterceptor() {
//            @Override
//            public Object intercept(Object obj, Method method, Object[] args,
//                    MethodProxy proxy) throws Throwable {
//                String name = method.getName();
//                Class<?>[] paramTypes = method.getParameterTypes();
//
//                InvokeData invokeData = new InvokeData(name, args, paramTypes);
//
//                Client client = new Client();
//                Object ret = client.transmitRpcCall(address, invokeData);
//
//                return ret;
//            }
//        });
//    }
}
