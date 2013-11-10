package com.offbynull.p2prpc.session;

import com.offbynull.p2prpc.transport.IncomingData;
import com.offbynull.p2prpc.transport.OutgoingData;
import com.offbynull.p2prpc.transport.SessionedTransport.RequestNotifier;
import com.offbynull.p2prpc.transport.SessionedTransport.RequestReceiver;
import com.offbynull.p2prpc.transport.SessionedTransport.ResponseSender;
import com.offbynull.p2prpc.transport.tcp.TcpTransport;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

public final class SessionedServer<A> implements Server<A> {

    private RequestNotifier notifier;
    private ServerMessageCallback<A> callback;
    private long timeout;
    
    private TcpRequestReceiver tcpRequestReceiver;

    public SessionedServer(TcpTransport transport, long timeout) {
        Validate.notNull(transport);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, timeout);
        
        notifier = transport.getRequestNotifier();
        this.timeout = timeout;
    }

    @Override
    public void start(ServerMessageCallback<A> callback) throws IOException {
        Validate.notNull(callback);
        
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
            Validate.notNull(data);
            Validate.notNull(responder);
            
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
            Validate.notNull(responder);
            Validate.notNull(requester);
            
            this.requester = requester;
            this.responder = responder;
            this.savedTime = time;
        }

        @Override
        public void responseReady(byte[] data) {
            Validate.notNull(data);
            
            long time = System.currentTimeMillis();
            if (time - savedTime < timeout && data != null) {
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
