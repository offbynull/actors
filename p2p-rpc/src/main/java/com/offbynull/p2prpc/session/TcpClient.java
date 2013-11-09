package com.offbynull.p2prpc.session;

import com.offbynull.p2prpc.transport.IncomingData;
import com.offbynull.p2prpc.transport.OutgoingData;
import com.offbynull.p2prpc.transport.SessionedTransport.RequestController;
import com.offbynull.p2prpc.transport.SessionedTransport.RequestSender;
import com.offbynull.p2prpc.transport.SessionedTransport.ResponseReceiver;
import com.offbynull.p2prpc.transport.tcp.TcpTransport;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class TcpClient implements Client<InetSocketAddress> {
    private RequestSender<InetSocketAddress> requestSender;

    public TcpClient(TcpTransport transport) {
        requestSender = transport.getRequestSender();
    }

    @Override
    public byte[] send(InetSocketAddress address, byte[] data, long timeout) throws IOException, InterruptedException {
        final ArrayBlockingQueue<byte[]> exchanger = new ArrayBlockingQueue<>(1); // exchanger/synchronousqueue shouldn't be used here due
                                                                                  // to potential of responseReceiver getting blocked
        
        ResponseReceiver<InetSocketAddress> responseReceiver = new ResponseReceiver<InetSocketAddress>() {

            @Override
            public void responseArrived(IncomingData<InetSocketAddress> data) {
                ByteBuffer recvData = data.getData();
                                    
                byte[] recvDataBytes = new byte[recvData.limit()];
                recvData.get(recvDataBytes);
                
                exchanger.add(recvDataBytes);
            }

            @Override
            public void communicationFailed() {
                exchanger.add(null);
            }
        };
        
        OutgoingData<InetSocketAddress> outgoingData = new OutgoingData<>(address, data);
        RequestController controller = requestSender.sendRequest(outgoingData, responseReceiver);

        try {
            byte[] recvData = exchanger.poll(timeout, TimeUnit.MILLISECONDS);
            
            if (recvData == null) {
                throw new IOException("Communcation failed");
            }
            
            return recvData;
        } finally {
            controller.killCommunication(); // just incase
        }
    }
}
