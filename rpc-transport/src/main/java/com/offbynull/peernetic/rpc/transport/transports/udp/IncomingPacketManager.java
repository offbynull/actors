package com.offbynull.peernetic.rpc.transport.transports.udp;

import com.offbynull.peernetic.common.concurrent.actor.helpers.TimeoutManager;
import com.offbynull.peernetic.common.concurrent.actor.helpers.TimeoutManager.TimeoutManagerResult;
import com.offbynull.peernetic.common.nio.utils.ByteBufferUtils;
import com.offbynull.peernetic.rpc.transport.IncomingFilter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class IncomingPacketManager<A> {
    private IncomingFilter<A> incomingFilter;
    private MessageIdCache<A> messageIdCache;

    private TimeoutManager<IncomingRequest<A>> pendingRequestTimeoutManager = new TimeoutManager<>();
    
    private List<IncomingRequest<A>> newRequests = new ArrayList<>();
    private List<IncomingResponse<A>> newResponses = new ArrayList<>();
    
    public void incomingData(A from, ByteBuffer data, long maxTimestamp) {
        ByteBuffer localTempBuffer = incomingFilter.filter(from, data);
        
        if (localTempBuffer == localTempBuffer) {
            localTempBuffer = ByteBufferUtils.copyContents(localTempBuffer);
        }

        if (MessageMarker.isRequest(localTempBuffer)) {
            MessageMarker.skipOver(localTempBuffer);

            MessageId messageId = MessageId.extractPrependedId(localTempBuffer);
            MessageId.skipOver(localTempBuffer);

            if (!messageIdCache.add(from, messageId, PacketType.RESPONSE)) {
                //throw new RuntimeException("Duplicate messageid encountered");
                return;
            }
            
            IncomingRequest<A> request = new IncomingRequest<>(from, localTempBuffer, messageId);
            
            pendingRequestTimeoutManager.add(request, maxTimestamp);
            newRequests.add(request);
        } else if (MessageMarker.isResponse(localTempBuffer)) {
            MessageMarker.skipOver(localTempBuffer);

            MessageId messageId = MessageId.extractPrependedId(localTempBuffer);
            MessageId.skipOver(localTempBuffer);

            if (!messageIdCache.add(from, messageId, PacketType.RESPONSE)) {
                //throw new RuntimeException("Duplicate messageid encountered");
                return;
            }
            
            IncomingResponse<A> response = new IncomingResponse<>(from, localTempBuffer, messageId);
            newResponses.add(response);
        }
    }
    
    public void executionComplete(someobjecthere id) {
        pendingRequestTimeoutManager.cancel(id);
    }
    
    public IncomingPacketManagerResult<A> process(long timestamp) {
        TimeoutManagerResult<IncomingRequest<A>> results = pendingRequestTimeoutManager.process(timestamp);
        
        IncomingPacketManagerResult<A> ret = new IncomingPacketManagerResult<>(newRequests, results.getTimedout(), newResponses,
                results.getNextTimeoutTimestamp());
        
        return ret;
    }
    
    private interface ReceiveEntity {
        
    }

    private static final class IncomingRequest<A> implements ReceiveEntity {
        private A from;
        private ByteBuffer data;
        private MessageId messageId;

        public IncomingRequest(A from, ByteBuffer data, MessageId messageId) {
            this.from = from;
            this.data = data;
            this.messageId = messageId;
        }

        public A getFrom() {
            return from;
        }

        public ByteBuffer getData() {
            return data;
        }

        public MessageId getMessageId() {
            return messageId;
        }
    }

    private static final class IncomingResponse<A> implements ReceiveEntity {
        private A from;
        private ByteBuffer data;
        private MessageId messageId;

        public IncomingResponse(A from, ByteBuffer data, MessageId messageId) {
            this.from = from;
            this.data = data;
            this.messageId = messageId;
        }

        public A getFrom() {
            return from;
        }

        public ByteBuffer getData() {
            return data;
        }

        public MessageId getMessageId() {
            return messageId;
        }
    }
    
    static final class IncomingPacketManagerResult<A> {
        private Collection<IncomingRequest<A>> newIncomingRequests;
        private Collection<IncomingRequest<A>> timedOutIncomingRequests;
        private Collection<IncomingResponse<A>> newIncomingResponses;
        private long nextTimeoutTimestamp;

        public IncomingPacketManagerResult(Collection<IncomingRequest<A>> newIncomingRequests,
                Collection<IncomingRequest<A>> timedOutIncomingRequests,
                Collection<IncomingResponse<A>> newIncomingResponses,
                long nextTimeoutTimestamp) {
            this.newIncomingRequests = newIncomingRequests;
            this.timedOutIncomingRequests = timedOutIncomingRequests;
            this.newIncomingResponses = newIncomingResponses;
            this.nextTimeoutTimestamp = nextTimeoutTimestamp;
        }

        public Collection<IncomingRequest<A>> getNewIncomingRequests() {
            return newIncomingRequests;
        }

        public Collection<IncomingRequest<A>> getTimedOutIncomingRequests() {
            return timedOutIncomingRequests;
        }

        public Collection<IncomingResponse<A>> getNewIncomingResponses() {
            return newIncomingResponses;
        }

        public long getNextTimeoutTimestamp() {
            return nextTimeoutTimestamp;
        }

    }
}
