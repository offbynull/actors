package com.offbynull.peernetic.demos.transport;

import com.offbynull.peernetic.rpc.TcpTransportFactory;
import com.offbynull.peernetic.rpc.transport.CompositeIncomingFilter;
import com.offbynull.peernetic.rpc.transport.CompositeOutgoingFilter;
import com.offbynull.peernetic.rpc.transport.IncomingFilter;
import com.offbynull.peernetic.rpc.transport.IncomingMessage;
import com.offbynull.peernetic.rpc.transport.IncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.IncomingMessageResponseHandler;
import com.offbynull.peernetic.rpc.transport.IncomingResponse;
import com.offbynull.peernetic.rpc.transport.OutgoingFilter;
import com.offbynull.peernetic.rpc.transport.OutgoingMessage;
import com.offbynull.peernetic.rpc.transport.OutgoingMessageResponseListener;
import com.offbynull.peernetic.rpc.transport.OutgoingResponse;
import com.offbynull.peernetic.rpc.transport.Transport;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

//
// http://stackoverflow.com/questions/10088363/java-net-socketexception-no-buffer-space-available-maximum-connections-reached
// http://support.microsoft.com/kb/2577795
//
// This has issues with TIME_WAIT.
public final class TcpTransportBenchmark {
    private static final int NUM_OF_TRANSPORTS = 2;
    private static Map<InetSocketAddress, Transport<InetSocketAddress>> transports = new HashMap<>();
    
    public static void main(String[] args) throws Throwable {
        for (int i = 0; i < NUM_OF_TRANSPORTS; i++) {
            final TcpTransportFactory tcpTransportFactory = new TcpTransportFactory();
            tcpTransportFactory.setListenAddress(new InetSocketAddress(InetAddress.getLocalHost(), 10000 + i));

            InetSocketAddress addr = new InetSocketAddress(InetAddress.getLocalHost(), 10000 + i);
            Transport transport = tcpTransportFactory.createTransport();
            transport.start(new CompositeIncomingFilter<>(Collections.<IncomingFilter<Integer>>emptyList()),
                    new EchoIncomingMessageListener(),
                    new CompositeOutgoingFilter<>(Collections.<OutgoingFilter<Integer>>emptyList()));
            transports.put(addr, transport);
        }
        
        for (int i = 0; i < NUM_OF_TRANSPORTS; i++) {
            for (int j = 0; j < NUM_OF_TRANSPORTS; j++) {
                if (i == j) {
                    continue;
                }
                
                InetSocketAddress fromAddr = new InetSocketAddress(InetAddress.getLocalHost(), 10000 + i);
                InetSocketAddress toAddr = new InetSocketAddress(InetAddress.getLocalHost(), 10000 + ((i + 1) % NUM_OF_TRANSPORTS));
                issueMessage(fromAddr, toAddr);
            }
        }
    }
    
    private static void issueMessage(InetSocketAddress from, InetSocketAddress to) {
        final long time = System.currentTimeMillis();

        ByteBuffer data = ByteBuffer.allocate(8);
        data.putLong(0, time);

        OutgoingMessage<InetSocketAddress> message = new OutgoingMessage<>(from, data);
        transports.get(to).sendMessage(message, new ReportAndReissueOutgoingMessageResponseListener(from, to));
    }

    private static final class EchoIncomingMessageListener implements IncomingMessageListener<InetSocketAddress> {

        @Override
        public void messageArrived(IncomingMessage<InetSocketAddress> message, IncomingMessageResponseHandler responseCallback) {
            ByteBuffer data = message.getData();
            responseCallback.responseReady(new OutgoingResponse(data));
        }
    }

    private static final class ReportAndReissueOutgoingMessageResponseListener
            implements OutgoingMessageResponseListener<InetSocketAddress> {
        private InetSocketAddress from;
        private InetSocketAddress to;

        public ReportAndReissueOutgoingMessageResponseListener(InetSocketAddress from, InetSocketAddress to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public void responseArrived(IncomingResponse<InetSocketAddress> response) {
            long diff = System.currentTimeMillis() - response.getData().getLong(0);
            System.out.println("Response time: " + diff);
            
            issueMessage(from, to);
        }

        @Override
        public void internalErrorOccurred(Throwable error) {
            System.err.println("ERROR: " + error);
            issueMessage(from, to);
        }

        @Override
        public void timedOut() {
            System.err.println("TIMEDOUT");
            issueMessage(from, to);
        }
    }
}
