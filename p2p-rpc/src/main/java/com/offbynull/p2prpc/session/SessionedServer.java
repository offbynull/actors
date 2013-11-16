package com.offbynull.p2prpc.session;

import com.offbynull.p2prpc.transport.IncomingData;
import com.offbynull.p2prpc.transport.OutgoingData;
import com.offbynull.p2prpc.transport.SessionedTransport;
import com.offbynull.p2prpc.transport.SessionedTransport.LinkController;
import com.offbynull.p2prpc.transport.SessionedTransport.RequestNotifier;
import com.offbynull.p2prpc.transport.SessionedTransport.RequestReceiver;
import com.offbynull.p2prpc.transport.SessionedTransport.ResponseSender;
import com.offbynull.p2prpc.transport.tcp.TcpTransport;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;

public final class SessionedServer<A> implements Server<A> {

    private RequestNotifier notifier;
    private ServerMessageCallback<A> callback;
    private long timeout;
    
    private Timer killTimer;
    
    private TcpRequestReceiver tcpRequestReceiver;
    
    private Lock startStopLock;

    public SessionedServer(TcpTransport transport, long timeout) {
        Validate.notNull(transport);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, timeout);
        
        notifier = transport.getRequestNotifier();
        this.timeout = timeout;
        
        startStopLock = new ReentrantLock();
    }

    @Override
    public void start(ServerMessageCallback<A> callback) throws IOException {
        Validate.notNull(callback);
        
        startStopLock.lock();
        try {
            killTimer = new Timer(getClass().getSimpleName() + " Kill Timer", true);
            
            this.callback = callback;
            tcpRequestReceiver = new TcpRequestReceiver();
        
            notifier.add(tcpRequestReceiver);
        } finally {
            startStopLock.unlock();
        }
    }

    @Override
    public void stop() throws IOException {
        startStopLock.lock();
        try {
            killTimer.cancel();
            notifier.remove(tcpRequestReceiver);
        } finally {
            startStopLock.unlock();
        }
    }

    private final class TcpRequestReceiver implements RequestReceiver<A> {

        @Override
        public boolean linkEstablished(A from, final SessionedTransport.LinkController controller) {
            killTimer.schedule(new TimerTask() {

                @Override
                public void run() {
                    controller.kill();
                }
            }, timeout);
            // do nothing
            return true;
        }

        @Override
        public boolean requestArrived(IncomingData<A> data, ResponseSender<A> responder, SessionedTransport.LinkController controller) {
            Validate.notNull(data);
            Validate.notNull(responder);
            Validate.notNull(controller);
            
            A from = data.getFrom();
            ByteBuffer recvData = data.getData();
            
            byte[] recvDataArray = new byte[recvData.limit()];
            recvData.get(recvDataArray);
            
            callback.messageArrived(from, recvDataArray, new ResponseCallback(responder, controller, from));
            
            return true;
        }
    }
    
    private final class ResponseCallback implements ServerResponseCallback {

        private A requester;
        private ResponseSender<A> responder;
        private LinkController controller;

        public ResponseCallback(ResponseSender<A> responder, LinkController controller, A requester) {
            Validate.notNull(responder);
            Validate.notNull(controller);
            Validate.notNull(requester);
            
            this.requester = requester;
            this.responder = responder;
            this.controller = controller;
        }

        @Override
        public void responseReady(byte[] data) {
            Validate.notNull(data);
            
            OutgoingData<A> outgoingData = new OutgoingData<>(requester, data);
            responder.sendResponse(outgoingData);
        }

        @Override
        public void terminate() {
            controller.kill();
        }
    }
}
