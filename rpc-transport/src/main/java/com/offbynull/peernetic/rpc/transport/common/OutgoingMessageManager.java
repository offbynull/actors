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
    private Map<Long, OutgoingRequest> requestMap = new HashMap<>();
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
        
        OutgoingRequest request = new OutgoingRequest(id, localTempBuffer, responder, to);
        
        
        queuedSends.add(request);
        queuedRequestTimeoutManager.add(id, queueTimestampTimeout);
        sentRequestTimeoutManager.add(id, sentTimestampTimeout);
        requestMap.put(id, request);
    }

    public void outgoingResponse(long id, A to, ByteBuffer data, MessageId messageId, long queueTimestampTimeout) {
        tempBuffer.clear();

        MessageMarker.writeRequestMarker(tempBuffer);
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
        SendEntity sendEntity = queuedSends.poll();
        
        if (sendEntity == null) {
            return null;
        }
        
        Packet<A> packet;
        if (sendEntity instanceof OutgoingMessageManager.OutgoingRequest) {
            OutgoingRequest req = (OutgoingRequest) sendEntity;
            queuedRequestTimeoutManager.cancel(req.getId());
            packet = new Packet<>(req.getData(), req.getDestination());
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
        long nextTimeoutTimestamp = Long.MAX_VALUE;
        
        TimeoutManagerResult<Long> timedOutQueuedRequests =  queuedRequestTimeoutManager.process(timestamp);
        for (Long id : timedOutQueuedRequests.getTimedout()) {
            sentRequestTimeoutManager.cancel(id);
            OutgoingRequest request = requestMap.remove(id);
            messageRespondersForFailures.add(request.getResponder());
        }
        nextTimeoutTimestamp = Math.min(nextTimeoutTimestamp, timedOutQueuedRequests.getNextTimeoutTimestamp());
        
        TimeoutManagerResult<Long> timedOutSentRequests = sentRequestTimeoutManager.process(timestamp);
        for (Long id : timedOutSentRequests.getTimedout()) {
            OutgoingRequest request = requestMap.remove(id);
            messageRespondersForFailures.add(request.getResponder());
        }
        nextTimeoutTimestamp = Math.min(nextTimeoutTimestamp, timedOutSentRequests.getNextTimeoutTimestamp());
        
        TimeoutManagerResult<Long> timedOutQueuedResponses = queuedResponseTimeoutManager.process(timestamp);
        nextTimeoutTimestamp = Math.min(nextTimeoutTimestamp, timedOutQueuedResponses.getNextTimeoutTimestamp());
        
        
        boolean hasMore = !messageRespondersForFailures.isEmpty();
        return new OutgoingPacketManagerResult(messageRespondersForFailures, nextTimeoutTimestamp, hasMore);
    }

    public MessageResponder responseReturned(long id) {
        sentRequestTimeoutManager.cancel(id);
        OutgoingRequest request = requestMap.remove(id);
        
        if (request == null) {
            return null;
        }
        
        return request.getResponder();
    }

    public static final class Packet<A> {
        private ByteBuffer data;
        private A destination;

        public Packet(ByteBuffer data, A destination) {
            this.data = data;
            this.destination = destination;
        }

        public ByteBuffer getData() {
            return data;
        }

        public A getDestination() {
            return destination;
        }
        
    }

    public interface SendEntity {
        
    }
    
    public final class OutgoingRequest implements SendEntity {
        private Long id;
        private ByteBuffer data;
        private MessageResponder responder;
        private A destination;

        public OutgoingRequest(Long id, ByteBuffer data, MessageResponder responder, A destination) {
            this.id = id;
            this.data = data;
            this.responder = responder;
            this.destination = destination;
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

        public A getDestination() {
            return destination;
        }
    }

    public final class OutgoingResponse implements SendEntity {
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
        private boolean morePacketsAvailable;

        private OutgoingPacketManagerResult(Collection<MessageResponder> messageRespondersForFailures, long nextTimeoutTimestamp,
                boolean morePacketsAvailable) {
            this.messageRespondersForFailures = Collections.unmodifiableCollection(messageRespondersForFailures);
            this.nextTimeoutTimestamp = nextTimeoutTimestamp;
            this.morePacketsAvailable = morePacketsAvailable;
        }

        public Collection<MessageResponder> getMessageRespondersForFailures() {
            return messageRespondersForFailures;
        }

        public long getNextTimeoutTimestamp() {
            return nextTimeoutTimestamp;
        }

        public boolean isMorePacketsAvailable() {
            return morePacketsAvailable;
        }
        
    }
}
