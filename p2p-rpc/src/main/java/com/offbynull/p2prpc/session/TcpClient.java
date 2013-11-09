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
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class TcpClient implements Client<InetSocketAddress> {
    private RequestSender<InetSocketAddress> requestSender;

    public TcpClient(TcpTransport transport) {
        requestSender = transport.getRequestSender();
    }

    @Override
    public byte[] send(InetSocketAddress address, byte[] data, long timeout) throws IOException, InterruptedException {
        final Exchanger<byte[]> exchanger = new Exchanger<>();
        
        ResponseReceiver<InetSocketAddress> responseReceiver = new ResponseReceiver<InetSocketAddress>() {

            @Override
            public void responseArrived(IncomingData<InetSocketAddress> data) {
                ByteBuffer recvData = data.getData();
                                    
                byte[] recvDataBytes = new byte[recvData.limit()];
                recvData.get(recvDataBytes);
                
                try {
                    exchanger.exchange(recvDataBytes, 0, TimeUnit.MILLISECONDS);
                } catch (InterruptedException | TimeoutException te) {
                    // Do nothing. The IE will have to be handled elsewhere. The TE should be gobbled.
                }
            }

            @Override
            public void communicationFailed() {
                try {
                    exchanger.exchange(null, 0, TimeUnit.MILLISECONDS);
                } catch (InterruptedException | TimeoutException te) {
                    // Do nothing. The IE will have to be handled elsewhere. The TE should be gobbled.
                }
            }
        };
        
        OutgoingData<InetSocketAddress> outgoingData = new OutgoingData<>(address, data);
        RequestController controller = requestSender.sendRequest(outgoingData, responseReceiver);

        try {
            byte[] recvData = exchanger.exchange(null, timeout, TimeUnit.MILLISECONDS);
            return recvData;
        } catch (TimeoutException te) {
            return null;
        } finally {
            controller.killCommunication();
        }
    }
}
