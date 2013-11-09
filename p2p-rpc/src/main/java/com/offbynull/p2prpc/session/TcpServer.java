package com.offbynull.p2prpc.session;

import com.offbynull.p2prpc.transport.IncomingData;
import com.offbynull.p2prpc.transport.OutgoingData;
import com.offbynull.p2prpc.transport.SessionedTransport.RequestNotifier;
import com.offbynull.p2prpc.transport.SessionedTransport.RequestReceiver;
import com.offbynull.p2prpc.transport.SessionedTransport.ResponseSender;
import com.offbynull.p2prpc.transport.tcp.TcpTransport;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public final class TcpServer implements Server<InetSocketAddress> {

    private RequestNotifier notifier;
    private ServerMessageCallback<InetSocketAddress> callback;
    private long timeout;
    
    private TcpRequestReceiver tcpRequestReceiver;

    public TcpServer(TcpTransport transport) {
        notifier = transport.getRequestNotifier();
    }

    @Override
    public void start(ServerMessageCallback<InetSocketAddress> callback) throws IOException {
        this.callback = callback;
        tcpRequestReceiver = new TcpRequestReceiver();
        
        notifier.add(tcpRequestReceiver);
    }

    @Override
    public void stop() throws IOException {
        notifier.remove(tcpRequestReceiver);
    }

    private final class TcpRequestReceiver implements RequestReceiver<InetSocketAddress> {

        @Override
        public boolean requestArrived(IncomingData<InetSocketAddress> data, ResponseSender<InetSocketAddress> responder) {
            InetSocketAddress from = data.getFrom();
            ByteBuffer recvData = data.getData();
            
            byte[] recvDataArray = new byte[recvData.limit()];
            recvData.get(recvDataArray);
            
            long time = System.currentTimeMillis();
            
            callback.messageArrived(from, recvDataArray, new ResponseCallback(time, responder, from));
            
            return true;
        }
    }
    
    private final class ResponseCallback implements ServerResponseCallback {

        private InetSocketAddress requester;
        private ResponseSender<InetSocketAddress> responder;
        private long savedTime;

        public ResponseCallback(long time, ResponseSender<InetSocketAddress> responder, InetSocketAddress requester) {
            this.requester = requester;
            this.responder = responder;
            this.savedTime = time;
        }

        @Override
        public void responseReady(byte[] data) {
            long time = System.currentTimeMillis();
            if (time - savedTime < timeout) {
                OutgoingData<InetSocketAddress> outgoingData = new OutgoingData<>(requester, data);
                responder.sendResponse(outgoingData);
            } else {
                responder.killCommunication();
            }
        }

        @Override
        public void terminate() {
            responder.killCommunication();
        }
    }
}
