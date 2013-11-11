package com.offbynull.p2prpc;

import com.offbynull.p2prpc.session.Client;
import com.offbynull.p2prpc.session.NonSessionedClient;
import com.offbynull.p2prpc.session.NonSessionedServer;
import com.offbynull.p2prpc.session.PacketIdGenerator;
import com.offbynull.p2prpc.session.Server;
import com.offbynull.p2prpc.session.SessionedClient;
import com.offbynull.p2prpc.session.SessionedServer;
import com.offbynull.p2prpc.transport.tcp.TcpTransport;
import com.offbynull.p2prpc.transport.udp.UdpTransport;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class SimpleRpc implements Closeable {

    private boolean closed;
    
    private Object transport;

    private Server<InetSocketAddress> server;
    private Client<InetSocketAddress> client;
    private ServiceServer<InetSocketAddress> serviceServer;
    private ServiceAccessor<InetSocketAddress> serviceAccessor;

    public SimpleRpc(int listenPort) throws IOException {
        this(TransportType.TCP, new InetSocketAddress(listenPort), Integer.MAX_VALUE);
    }

    public SimpleRpc(TransportType transportType, int listenPort) throws IOException {
        this(transportType, new InetSocketAddress(listenPort), Integer.MAX_VALUE);
    }

    public SimpleRpc(TransportType transportType, InetSocketAddress listenAddress) throws IOException {
        this(transportType, listenAddress, Integer.MAX_VALUE);
    }

    public SimpleRpc(TransportType transportType, int listenPort, int maxThreadCount) throws IOException {
        this(transportType, new InetSocketAddress(listenPort), maxThreadCount);
    }

    public SimpleRpc(TransportType transportType, InetSocketAddress listenAddress, int maxThreadCount) throws IOException {

        try {
            switch (transportType) {
                case TCP: {
                    TcpTransport tcpTransport = new TcpTransport(listenAddress);
                    tcpTransport.start();
                    server = new SessionedServer<>(tcpTransport, 10000L);
                    client = new SessionedClient<>(tcpTransport);
                    transport = tcpTransport;
                    break;
                }
                case UDP: {
                    UdpTransport udpTransport = new UdpTransport(listenAddress, 65535);
                    udpTransport.start();
                    server = new NonSessionedServer<>(udpTransport, 10000L);
                    client = new NonSessionedClient<>(udpTransport, new PacketIdGenerator());
                    transport = udpTransport;
                    break;
                }
                default:
                    throw new IllegalArgumentException();
            }

            serviceServer = new ServiceServer<>(server, new ThreadPoolExecutor(0, maxThreadCount, 1000L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>()));
            serviceAccessor = new ServiceAccessor<>(client);
            
            serviceServer.start();
            
            closed = false;
        } catch (IOException | RuntimeException ex) {
            closed = true;
            close();
            throw ex;
        }
    }

    private void stopTransport() throws IOException {
        if (transport instanceof TcpTransport) {
            ((TcpTransport) transport).stop();
        } else if (transport instanceof UdpTransport) {
            ((UdpTransport) transport).stop();
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public void close() {
        closed = true;
        
        try {
            serviceServer.stop();
        } catch (Exception ex) {
            // do nothing
        }

        try {
            stopTransport();
        } catch (Exception ex) {
            // do nothing
        }
    }

    public void addService(int id, Object object) {
        if (closed) {
            throw new IllegalStateException();
        }
        
        serviceServer.addService(id, object);
    }

    public void removeService(int id) {
        if (closed) {
            throw new IllegalStateException();
        }
        
        serviceServer.removeService(id);
    }

    public <T> T accessService(InetSocketAddress address, int serviceId, Class<T> type) {
        if (closed) {
            throw new IllegalStateException();
        }
        
        return serviceAccessor.accessService(address, serviceId, type);
    }

    public <T> T accessService(InetSocketAddress address, int serviceId, Class<T> type, long timeout) {
        if (closed) {
            throw new IllegalStateException();
        }
        
        return serviceAccessor.accessService(address, serviceId, type, timeout);
    }

    public <T> T accessService(InetSocketAddress address, int serviceId, Class<T> type, long timeout,
            RuntimeException throwOnCommFailure, RuntimeException throwOnInvokeFailure) {
        if (closed) {
            throw new IllegalStateException();
        }
        
        return serviceAccessor.accessService(address, serviceId, type, timeout, throwOnCommFailure, throwOnInvokeFailure);
    }

    public enum TransportType {

        TCP,
        UDP
    }
}
