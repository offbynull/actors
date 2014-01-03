package com.offbynull.peernetic.rpc.transport.common;

import com.offbynull.peernetic.common.concurrent.actor.Message.MessageResponder;
import com.offbynull.peernetic.common.concurrent.actor.helpers.TimeoutManager;
import com.offbynull.peernetic.common.concurrent.actor.helpers.TimeoutManager.TimeoutManagerResult;
import com.offbynull.peernetic.common.nio.utils.ByteBufferUtils;
import com.offbynull.peernetic.rpc.transport.OutgoingFilter;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class OutgoingMessageManager<A> {
    private ByteBuffer tempBuffer;

    private OutgoingFilter<A> outgoingFilter;
    
    private MessageIdGenerator idGenerator = new MessageIdGenerator();
    
    private TimeoutManager<Long> queuedRequestTimeoutManager = new TimeoutManager<>();
    private TimeoutManager<Long> sentRequestTimeoutManager = new TimeoutManager<>();
    private Map<Long, OutgoingRequest> requestIdLookup = new HashMap<>();
    private Map<MessageId, Long> requestMessageIdLookup = new HashMap<>();
    private TimeoutManager<Long> queuedResponseTimeoutManager = new TimeoutManager<>();
    private LinkedList<SendEntity> queuedSends = new LinkedList<>();

    public OutgoingMessageManager(int bufferSize, OutgoingFilter<A> outgoingFilter) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, bufferSize);
        Validate.notNull(outgoingFilter);
        
        this.tempBuffer = ByteBuffer.allocate(bufferSize);
        this.outgoingFilter = outgoingFilter;
    }
    
    public void outgoingRequest(long id, A to, ByteBuffer data, long queueTimestampTimeout, long sentTimestampTimeout,
            MessageResponder responder) {
        Validate.notNull(to);
        Validate.notNull(data);
        Validate.notNull(responder);

        MessageId messageId = idGenerator.generate();

        tempBuffer.clear();

        MessageMarker.writeRequestMarker(tempBuffer);
        messageId.writeId(tempBuffer);
        tempBuffer.put(data);
        tempBuffer.flip();

        ByteBuffer localTempBuffer = outgoingFilter.filter(to, tempBuffer);
        
        if (localTempBuffer == tempBuffer) {
            localTempBuffer = ByteBufferUtils.copyContents(tempBuffer);
        }
        
        OutgoingRequest request = new OutgoingRequest(id, messageId, localTempBuffer, responder, to);
        
        if (requestMessageIdLookup.containsKey(messageId)) {
            // Duplicate hit. Don't add, just ignore
            return;
        }
        
        queuedSends.add(request);
        queuedRequestTimeoutManager.add(id, queueTimestampTimeout);
        sentRequestTimeoutManager.add(id, sentTimestampTimeout);
        requestIdLookup.put(id, request);
        requestMessageIdLookup.put(messageId, id);
    }

    public void outgoingResponse(long id, A to, ByteBuffer data, MessageId messageId, long queueTimestampTimeout) {
        Validate.notNull(to);
        Validate.notNull(data);
        Validate.notNull(messageId);
        
        tempBuffer.clear();

        MessageMarker.writeResponseMarker(tempBuffer);
        messageId.writeId(tempBuffer);
        tempBuffer.put(data);
        tempBuffer.flip();

        ByteBuffer localTempBuffer = outgoingFilter.filter(to, tempBuffer);
        
        if (localTempBuffer == tempBuffer) {
            localTempBuffer = ByteBufferUtils.copyContents(tempBuffer);
        }

        OutgoingResponse response = new OutgoingResponse(id, localTempBuffer, to);
        
        
        queuedSends.add(response);
    }

    public Packet<A> getNextOutgoingPacket() {
        SendEntity sendEntity = null;
        while (true) {
            sendEntity = queuedSends.poll();
            
            if (sendEntity == null) {
                return null;
            }
            
            if (!sendEntity.isCancelled()) {
                break;
            }
        }
        
        Packet<A> packet;
        if (sendEntity instanceof OutgoingMessageManager.OutgoingRequest) {
            OutgoingRequest req = (OutgoingRequest) sendEntity;
            queuedRequestTimeoutManager.cancel(req.getId());
            packet = new Packet<>(req.getData(), req.getTo());
        } else if (sendEntity instanceof OutgoingMessageManager.OutgoingResponse) {
            OutgoingResponse resp = (OutgoingResponse) sendEntity;
            packet = new Packet<>(resp.getData(), resp.getDestination());
        } else {
            throw new IllegalStateException();
        }
        
        return packet;
    }

    public OutgoingPacketManagerResult process(long timestamp) {
        Set<MessageResponder> messageRespondersForFailures = new HashSet<>();
        long maxTimestamp = Long.MAX_VALUE;
        
        TimeoutManagerResult<Long> timedOutQueuedRequests =  queuedRequestTimeoutManager.process(timestamp);
        for (Long id : timedOutQueuedRequests.getTimedout()) {
            sentRequestTimeoutManager.cancel(id);
            OutgoingRequest request = requestIdLookup.remove(id);
            requestMessageIdLookup.remove(request.getMessageId());
            messageRespondersForFailures.add(request.getResponder());
            request.cancel();
        }
        maxTimestamp = Math.min(maxTimestamp, timedOutQueuedRequests.getNextMaxTimestamp());
        
        TimeoutManagerResult<Long> timedOutSentRequests = sentRequestTimeoutManager.process(timestamp);
        for (Long id : timedOutSentRequests.getTimedout()) {
            OutgoingRequest request = requestIdLookup.remove(id);
            requestMessageIdLookup.remove(request.getMessageId());
            messageRespondersForFailures.add(request.getResponder());
            request.cancel();
        }
        maxTimestamp = Math.min(maxTimestamp, timedOutSentRequests.getNextMaxTimestamp());
        
        TimeoutManagerResult<Long> timedOutQueuedResponses = queuedResponseTimeoutManager.process(timestamp);
        maxTimestamp = Math.min(maxTimestamp, timedOutQueuedResponses.getNextMaxTimestamp());
        
        return new OutgoingPacketManagerResult(messageRespondersForFailures, maxTimestamp, queuedSends.size());
    }

    public MessageResponder responseReturned(MessageId messageId) {
        Long internalId = requestMessageIdLookup.remove(messageId);
        if (internalId == null) {
            return null;
        }
        
        sentRequestTimeoutManager.cancel(internalId);
        OutgoingRequest request = requestIdLookup.remove(internalId);
        
        if (request == null) {
            return null;
        }
        
        return request.getResponder();
    }

    public static final class Packet<A> {
        private ByteBuffer data;
        private A to;

        public Packet(ByteBuffer data, A to) {
            Validate.notNull(data);
            Validate.notNull(to);
            
            this.data = data;
            this.to = to;
        }

        public ByteBuffer getData() {
            return data;
        }

        public A getTo() {
            return to;
        }
        
    }

    public abstract class SendEntity {
        private boolean cancelled;

        public boolean isCancelled() {
            return cancelled;
        }

        public void cancel() {
            this.cancelled = true;
        }
        
    }
    
    public final class OutgoingRequest extends SendEntity {
        private Long id;
        private MessageId messageId;
        private ByteBuffer data;
        private MessageResponder responder;
        private A to;

        public OutgoingRequest(Long id, MessageId messageId, ByteBuffer data, MessageResponder responder, A to) {
            this.id = id;
            this.data = data;
            this.responder = responder;
            this.to = to;
            this.messageId = messageId;
        }

        public Long getId() {
            return id;
        }

        public ByteBuffer getData() {
            return data;
        }

        public MessageResponder getResponder() {
            return responder;
        }

        public MessageId getMessageId() {
            return messageId;
        }

        public A getTo() {
            return to;
        }
    }

    public final class OutgoingResponse extends SendEntity {
        private Long id;
        private ByteBuffer data;
        private A destination;

        public OutgoingResponse(Long id, ByteBuffer data, A destination) {
            this.id = id;
            this.data = data;
            this.destination = destination;
        }

        public Long getId() {
            return id;
        }

        public ByteBuffer getData() {
            return data;
        }

        public A getDestination() {
            return destination;
        }
    }
    
    public static final class OutgoingPacketManagerResult {
        private Collection<MessageResponder> messageRespondersForFailures;
        private long nextTimeoutTimestamp;
        private int packetsAvailable;

        private OutgoingPacketManagerResult(Collection<MessageResponder> messageRespondersForFailures, long nextTimeoutTimestamp,
                int packetsAvailable) {
            this.messageRespondersForFailures = Collections.unmodifiableCollection(messageRespondersForFailures);
            this.nextTimeoutTimestamp = nextTimeoutTimestamp;
            this.packetsAvailable = packetsAvailable;
        }

        public Collection<MessageResponder> getMessageRespondersForFailures() {
            return messageRespondersForFailures;
        }

        public long getNextTimeoutTimestamp() {
            return nextTimeoutTimestamp;
        }

        public int getPacketsAvailable() {
            return packetsAvailable;
        }
        
    }
}
