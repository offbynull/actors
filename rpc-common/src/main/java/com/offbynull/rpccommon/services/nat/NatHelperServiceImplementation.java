package com.offbynull.rpccommon.services.nat;

import com.offbynull.rpc.RpcInvokeKeys;
import com.offbynull.rpc.invoke.InvokeThreadInformation;
import com.offbynull.rpc.transport.CompositeIncomingFilter;
import com.offbynull.rpc.transport.CompositeOutgoingFilter;
import com.offbynull.rpc.transport.IncomingFilter;
import com.offbynull.rpc.transport.OutgoingFilter;
import com.offbynull.rpc.transport.OutgoingMessage;
import com.offbynull.rpc.transport.TerminateIncomingMessageListener;
import com.offbynull.rpc.transport.Transport;
import com.offbynull.rpc.transport.TransportHelper;
import com.offbynull.rpc.transport.tcp.TcpTransport;
import com.offbynull.rpc.transport.udp.UdpTransport;
import java.net.InetSocketAddress;
import java.util.Collections;

public final class NatHelperServiceImplementation implements NatHelperService {

    private static final IncomingFilter<InetSocketAddress> EMPTY_INCOMING_FILTER =
            new CompositeIncomingFilter<>(Collections.<IncomingFilter<InetSocketAddress>>emptyList());
    private static final OutgoingFilter<InetSocketAddress> EMPTY_OUTGOING_FILTER =
            new CompositeOutgoingFilter<>(Collections.<OutgoingFilter<InetSocketAddress>>emptyList());
    
    @Override
    public String getAddress() {
        Object from = InvokeThreadInformation.getInfo(RpcInvokeKeys.FROM_ADDRESS);
        
        if (!(from instanceof InetSocketAddress)) {
            return null;
        }
        
        InetSocketAddress inetFrom = (InetSocketAddress) from;
        return inetFrom.getAddress().getHostAddress() + " " + inetFrom.getPort();
    }

    @Override
    public TestPortResult testPort(ConnectionType type, int port, byte[] challenge) {
        Transport transport = null;
        try {
            if (challenge.length != 8) {
                throw new RuntimeException();
            }

            Object from = InvokeThreadInformation.getInfo(RpcInvokeKeys.FROM_ADDRESS);
            if (!(from instanceof InetSocketAddress)) {
                return TestPortResult.ERROR;
            }
            InetSocketAddress inetFrom = (InetSocketAddress) from;

            switch (type) {
                case TCP:
                    transport = new TcpTransport(0, 1024, 1024, 10000L);
                    break;
                case UDP:
                    transport = new UdpTransport(0, 1024, 1024, 10000L);
                    break;
                default:
                    return TestPortResult.ERROR;
            }
            
            transport.start(EMPTY_INCOMING_FILTER, new TerminateIncomingMessageListener<InetSocketAddress>(), EMPTY_OUTGOING_FILTER);
            
            InetSocketAddress inetTo = new InetSocketAddress(inetFrom.getAddress(), port);
            OutgoingMessage<InetSocketAddress> outgoingMessage = new OutgoingMessage<>(inetTo, challenge);
            TransportHelper.sendAndForget(transport, outgoingMessage);

            return TestPortResult.SUCCESS; // sendAndForget will close the transport for us once it's finished
        } catch (Exception e) {
            if (transport != null) {
                try {
                    transport.stop();
                } catch (Exception ex) {
                    // do nothing
                }
            }
            
            return TestPortResult.ERROR;
        }
    }
    
}
