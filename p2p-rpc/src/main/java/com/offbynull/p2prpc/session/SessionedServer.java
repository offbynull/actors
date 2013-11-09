package com.offbynull.p2prpc.session;

import com.offbynull.p2prpc.transport.IncomingData;
import com.offbynull.p2prpc.transport.OutgoingData;
import com.offbynull.p2prpc.transport.SessionedTransport.RequestNotifier;
import com.offbynull.p2prpc.transport.SessionedTransport.RequestReceiver;
import com.offbynull.p2prpc.transport.SessionedTransport.ResponseSender;
import com.offbynull.p2prpc.transport.tcp.TcpTransport;
import java.io.IOException;
import java.nio.ByteBuffer;

public final class SessionedServer<A> implements Server<A> {

    private RequestNotifier notifier;
    private ServerMessageCallback<A> callback;
    private long timeout;
    
    private TcpRequestReceiver tcpRequestReceiver;

    public SessionedServer(TcpTransport transport, long timeout) {
        notifier = transport.getRequestNotifier();
        this.timeout = timeout;
    }

    @Override
    public void start(ServerMessageCallback<A> callback) throws IOException {
        this.callback = callback;
        tcpRequestReceiver = new TcpRequestReceiver();
        
        notifier.add(tcpRequestReceiver);
    }

    @Override
    public void stop() throws IOException {
        notifier.remove(tcpRequestReceiver);
    }

    private final class TcpRequestReceiver implements RequestReceiver<A> {

        @Override
        public boolean requestArrived(IncomingData<A> data, ResponseSender<A> responder) {
            A from = data.getFrom();
            ByteBuffer recvData = data.getData();
            
            byte[] recvDataArray = new byte[recvData.limit()];
            recvData.get(recvDataArray);
            
            long time = System.currentTimeMillis();
            
            callback.messageArrived(from, recvDataArray, new ResponseCallback(time, responder, from));
            
            return true;
        }
    }
    
    private final class ResponseCallback implements ServerResponseCallback {

        private A requester;
        private ResponseSender<A> responder;
        private long savedTime;

        public ResponseCallback(long time, ResponseSender<A> responder, A requester) {
            this.requester = requester;
            this.responder = responder;
            this.savedTime = time;
        }

        @Override
        public void responseReady(byte[] data) {
            long time = System.currentTimeMillis();
            if (time - savedTime < timeout) {
                OutgoingData<A> outgoingData = new OutgoingData<>(requester, data);
                responder.sendResponse(outgoingData);
            } else {
                responder.killConnection();
            }
        }

        @Override
        public void terminate() {
            responder.killConnection();
        }
    }
}
