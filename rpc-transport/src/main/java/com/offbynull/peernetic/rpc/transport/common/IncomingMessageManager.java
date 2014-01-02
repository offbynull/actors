package com.offbynull.peernetic.rpc.transport.common;

import com.offbynull.peernetic.common.concurrent.actor.helpers.TimeoutManager;
import com.offbynull.peernetic.common.concurrent.actor.helpers.TimeoutManager.TimeoutManagerResult;
import com.offbynull.peernetic.common.nio.utils.ByteBufferUtils;
import com.offbynull.peernetic.rpc.transport.IncomingFilter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class IncomingMessageManager<A> {
    private IncomingFilter<A> incomingFilter;
    private MessageIdCache<A> messageIdCache = new MessageIdCache<>(1024);

    private TimeoutManager<Long> pendingRequestTimeoutManager = new TimeoutManager<>();
    private Map<Long, PendingRequest<A>> pendingRequestLookup = new HashMap<>();
    
    private List<IncomingRequest<A>> newRequests = new ArrayList<>();
    private List<IncomingResponse<A>> newResponses = new ArrayList<>();

    public IncomingMessageManager(IncomingFilter<A> incomingFilter) {
        Validate.notNull(incomingFilter);
        
        this.incomingFilter = incomingFilter;
    }
    
    public void incomingData(long id, A from, ByteBuffer data, long maxTimestamp) {
        ByteBuffer localTempBuffer = incomingFilter.filter(from, data);
        
        if (localTempBuffer == localTempBuffer) {
            localTempBuffer = ByteBufferUtils.copyContents(localTempBuffer);
        }

        if (MessageMarker.isRequest(localTempBuffer)) {
            MessageMarker.skipOver(localTempBuffer);

            MessageId messageId = MessageId.extractPrependedId(localTempBuffer);
            MessageId.skipOver(localTempBuffer);

            if (!messageIdCache.add(from, messageId, MessageType.REQUEST)) {
                //throw new RuntimeException("Duplicate messageid encountered");
                return;
            }
            
            IncomingRequest<A> request = new IncomingRequest<>(id, from, localTempBuffer, messageId);
            PendingRequest<A> pending = new PendingRequest<>(id, from, messageId);
            
            pendingRequestTimeoutManager.add(id, maxTimestamp);
            pendingRequestLookup.put(id, pending);
            newRequests.add(request);
        } else if (MessageMarker.isResponse(localTempBuffer)) {
            MessageMarker.skipOver(localTempBuffer);

            MessageId messageId = MessageId.extractPrependedId(localTempBuffer);
            MessageId.skipOver(localTempBuffer);

            if (!messageIdCache.add(from, messageId, MessageType.RESPONSE)) {
                //throw new RuntimeException("Duplicate messageid encountered");
                return;
            }
            
            IncomingResponse<A> response = new IncomingResponse<>(id, from, localTempBuffer, messageId);
            newResponses.add(response);
        }
    }
    
    public PendingRequest<A> responseFormed(long id) {
        pendingRequestTimeoutManager.cancel(id);
        return pendingRequestLookup.remove(id);
    }
    
    public IncomingPacketManagerResult<A> process(long timestamp) {
        TimeoutManagerResult<Long> results = pendingRequestTimeoutManager.process(timestamp);
        Set<Long> timedOutIds = results.getTimedout();
        
        Set<PendingRequest<A>> timedOutPendingRequests = new HashSet<>();
        for (Long timedOutId : timedOutIds) {
            timedOutPendingRequests.add(pendingRequestLookup.remove(timedOutId));
        }
        
        IncomingPacketManagerResult<A> ret = new IncomingPacketManagerResult<>(
                newRequests,
                timedOutPendingRequests,
                newResponses,
                results.getNextTimeoutTimestamp());
        
        newRequests = new LinkedList<>();
        newResponses = new LinkedList<>();
        
        return ret;
    }
    
    private interface ReceiveEntity {
        
    }

    public static final class IncomingRequest<A> implements ReceiveEntity {
        private long id;
        private A from;
        private ByteBuffer data;
        private MessageId messageId;

        public IncomingRequest(long id, A from, ByteBuffer data, MessageId messageId) {
            this.id = id;
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

        public long getId() {
            return id;
        }
    }

    public static final class PendingRequest<A> implements ReceiveEntity {
        private long id;
        private A from;
        private MessageId messageId;

        public PendingRequest(long id, A from, MessageId messageId) {
            this.id = id;
            this.from = from;
            this.messageId = messageId;
        }

        public A getFrom() {
            return from;
        }

        public MessageId getMessageId() {
            return messageId;
        }

        public long getId() {
            return id;
        }
    }
    
    public static final class IncomingResponse<A> implements ReceiveEntity {
        private long id;
        private A from;
        private ByteBuffer data;
        private MessageId messageId;

        public IncomingResponse(long id, A from, ByteBuffer data, MessageId messageId) {
            this.id = id;
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

        public long getId() {
            return id;
        }
    }
    
    public static final class IncomingPacketManagerResult<A> {
        private Collection<IncomingRequest<A>> newIncomingRequests;
        private Collection<PendingRequest<A>> timedOutIncomingRequests;
        private Collection<IncomingResponse<A>> newIncomingResponses;
        private long nextTimeoutTimestamp;

        public IncomingPacketManagerResult(Collection<IncomingRequest<A>> newIncomingRequests,
                Collection<PendingRequest<A>> timedOutIncomingRequests,
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

        public Collection<PendingRequest<A>> getTimedOutIncomingRequests() {
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
