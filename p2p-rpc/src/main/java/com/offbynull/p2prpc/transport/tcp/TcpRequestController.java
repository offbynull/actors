package com.offbynull.p2prpc.transport.tcp;

import com.offbynull.p2prpc.transport.SessionedTransport;
import java.nio.channels.Selector;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.lang3.Validate;

final class TcpRequestController implements SessionedTransport.RequestController {
        private long id;
        private Selector selector;
        private LinkedBlockingQueue<OutgoingRequest> outgoingData;

        public TcpRequestController(long id, Selector selector, LinkedBlockingQueue<OutgoingRequest> outgoingData) {
            Validate.notNull(selector);
            Validate.notNull(outgoingData);
            this.id = id;
            this.selector = selector;
            this.outgoingData = outgoingData;
        }



        @Override
        public void killCommunication() {
            outgoingData.add(new KillQueuedRequest(id));
            selector.wakeup();
        }
        
}
