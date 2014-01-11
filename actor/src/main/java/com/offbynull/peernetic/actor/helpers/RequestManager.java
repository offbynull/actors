/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.actor.helpers;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.PushQueue;
import com.offbynull.peernetic.actor.helpers.TimeoutManager.TimeoutManagerResult;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.commons.lang3.Validate;

/**
 * Provides a request-response abstraction around messages.
 * @author Kasra Faghihi
 */
public final class RequestManager {

    private Random random;

    // request related
    private Map<ByteBuffer, RequestEntity> requestKeys;
    private Map<RequestObject, RequestEntity> requests;
    private TimeoutManager<RequestObject> requestTimeoutManager;

    // response related
    private Map<Class<?>, IncomingRequestHandler<?>> requestHandlerMap;
    private List<ResponseEntity> responses;

    /**
     * Construct a {@link RequestManager} object.
     * @param random secure random used to generate keys that identify request and response messages
     * @throws NullPointerException if any arguments are {@code null}
     */
    public RequestManager(Random random) {
        Validate.notNull(random);
        this.random = random;
        
        this.requestKeys = new HashMap<>();
        this.requests = new HashMap<>();
        this.requestTimeoutManager = new TimeoutManager<>();
        
        this.requestHandlerMap = new HashMap<>();
        this.responses = new LinkedList<>();
    }

    /**
     * Queues up a new outgoing request.
     * @param <T> request type
     * @param timestamp current timestamp
     * @param content message
     * @param destination destination
     * @param handler handler that gets notified once a response is in or a timeout occurs
     * @param timeout amount of time to wait before retrying or failing
     * @param maxSendAttempts maximum send attempts before failing
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if {@code timeout < 0} or {@code maxSendAttempts < 1}
     */
    public <T> void newRequest(long timestamp, T content, Endpoint destination, OutgoingRequestHandler<T> handler, long timeout,
            int maxSendAttempts) {
        Validate.notNull(content);
        Validate.notNull(destination);
        Validate.notNull(handler);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, maxSendAttempts);
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, timeout);

        RequestObject request = new RequestObject(random, timestamp, content);

        RequestEntity entity = new RequestEntity(request, destination, handler, maxSendAttempts, timeout);
        requests.put(request, entity);
        requestKeys.put(request.getKey(), entity);
        requestTimeoutManager.add(request, timestamp); // looks wrong, but actually correct. upon adding will be "timed out", but will
                                                       // increment send attempts and send once it hits process -- maxSendAttempts will
                                                       // always be >= 1, so it will always get sent at least once
    }

    /**
     * Map a request handler. Incoming requests of the specified type will be processed by the request handler.
     * @param <T> message type
     * @param messageType message type
     * @param handler handler that gets called when a request message of this type comes in
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if {@code messageType} has already been mapped
     */
    public <T> void mapRequestHandler(Class<T> messageType, IncomingRequestHandler<T> handler) {
        Validate.notNull(messageType);
        Validate.notNull(handler);
        Validate.isTrue(!requestHandlerMap.containsKey(messageType));

        requestHandlerMap.put(messageType, handler);
    }

    /**
     * Unmap a request handler.
     * @param <T> message type
     * @param messageType message type
     * @throws NullPointerException if any arguments are {@code null}
     */
    public <T> void unmapRequestHandler(Class<T> messageType) {
        Validate.notNull(messageType);

        requestHandlerMap.remove(messageType);
    }

    /**
     * Process an incoming message. If the message is a request, it gets piped to the whatever {@link IncomingRequestHandler} is mapped to
     * the type (if any). If the message is a response, it gets piped to the {@link OutgoingRequestHandler} for the request it was sent for
     * (if it's still active -- may have timed out). If a message is neither a request nor a response, it's ignored.
     * @param timestamp current timestamp
     * @param incoming incoming message
     * @throws NullPointerException if any arguments are {@code null}
     */
    public void incomingMessage(long timestamp, Incoming incoming) {
        Validate.notNull(incoming);

        Object content = incoming.getContent();
        Endpoint source = incoming.getSource();

        if (content instanceof RequestObject) {
            RequestObject requestObject = (RequestObject) content;

            Object requestContent = requestObject.getContent();
            IncomingRequestHandler handler = requestHandlerMap.get(requestContent.getClass());
            if (handler == null) {
                return;
            }

            Object responseContent = handler.produceResponse(timestamp, requestContent);
            if (responseContent == null) {
                return;
            }

            ResponseObject wrappedResponseObject = new ResponseObject(requestObject, responseContent);
            responses.add(new ResponseEntity(wrappedResponseObject, source));
        } else if (content instanceof ResponseObject) {
            ResponseObject responseObject = (ResponseObject) content;

            ByteBuffer key = responseObject.getKey();
            RequestEntity entity = requestKeys.get(key);
            if (entity == null) {
                return;
            }

            OutgoingRequestHandler handler = entity.getHandler();

            requestKeys.remove(key);
            requests.remove(entity.getRequestObject());
            requestTimeoutManager.cancel(entity.getRequestObject());
            
            handler.responseComplete(entity.getRequestObject().getContent(), responseObject.getContent());
        }
    }

    /**
     * Process the request manager. Call to time out requests and push out requests/responses.
     * @param timestamp current timestamp
     * @param pushQueue push queue
     * @return next timeout timestamp for a request
     */
    public long process(long timestamp, PushQueue pushQueue) {
        TimeoutManagerResult<RequestObject> result = requestTimeoutManager.process(timestamp);

        for (RequestObject request : result.getTimedout()) {
            RequestEntity entity = requests.get(request);

            if (!entity.incrementSendAttempt()) {
                requestTimeoutManager.add(request, timestamp + entity.getTimeout());
                pushQueue.push(entity.getEndpoint(), entity.getRequestObject());
            } else {
                requests.remove(request);
                entity.getHandler().responseTimedOut(request.getContent());
            }
        }

        for (ResponseEntity entity : responses) {
            pushQueue.push(entity.getEndpoint(), entity.getResponseObject());
        }
        responses.clear();

        return result.getNextTimeoutTimestamp();
    }

    private static final class RequestEntity {

        private RequestObject content;
        private Endpoint endpoint;
        private OutgoingRequestHandler handler;
        private int maxSendAttempts;
        private int currentSendAttempts;
        private long timeout;

        public RequestEntity(RequestObject content, Endpoint endpoint, OutgoingRequestHandler handler, int maxSendAttempts, long timeout) {
            Validate.notNull(content);
            Validate.notNull(endpoint);
            Validate.notNull(handler);
            Validate.inclusiveBetween(1, Integer.MAX_VALUE, maxSendAttempts);
            Validate.inclusiveBetween(0L, Long.MAX_VALUE, timeout);

            this.content = content;
            this.endpoint = endpoint;
            this.handler = handler;
            this.maxSendAttempts = maxSendAttempts;
            this.timeout = timeout;
        }

        public RequestObject getRequestObject() {
            return content;
        }

        public boolean incrementSendAttempt() {
            currentSendAttempts++;
            return currentSendAttempts > maxSendAttempts;
        }

        public long getTimeout() {
            return timeout;
        }

        public Endpoint getEndpoint() {
            return endpoint;
        }

        public OutgoingRequestHandler getHandler() {
            return handler;
        }

    }

    private static final class ResponseEntity {

        private ResponseObject content;
        private Endpoint endpoint;

        public ResponseEntity(ResponseObject content, Endpoint endpoint) {
            Validate.notNull(content);
            Validate.notNull(endpoint);

            this.content = content;
            this.endpoint = endpoint;
        }

        public ResponseObject getResponseObject() {
            return content;
        }

        public Endpoint getEndpoint() {
            return endpoint;
        }

    }

    private static final class RequestObject {

        private ByteBuffer key;
        private Object content;

        public RequestObject(Random random, long marker, Object content) {
            Validate.notNull(random);
            Validate.notNull(content);

            byte[] randomData = new byte[8];
            random.nextBytes(randomData);

            this.key = ByteBuffer.allocate(16);
            key.put(randomData).putLong(marker).flip();

            this.content = content;
        }

        public ByteBuffer getKey() {
            return key.asReadOnlyBuffer();
        }

        public Object getContent() {
            return content;
        }

    }

    private static final class ResponseObject {

        private ByteBuffer key;
        private Object content;

        public ResponseObject(RequestObject requestObject, Object content) {
            Validate.notNull(requestObject);
            Validate.notNull(content);

            this.key = requestObject.getKey();
            this.content = content;
        }

        public ByteBuffer getKey() {
            return key.asReadOnlyBuffer();
        }

        public Object getContent() {
            return content;
        }

    }

    /**
     * Handles generating responses for incoming requests.
     * @param <T> message type
     */
    public interface IncomingRequestHandler<T> {

        /**
         * Indicates that a request has come in. Should produce a response for the request.
         * @param timestamp current time
         * @param request request message
         * @return response message
         * @throws NullPointerException if any argument is {@code null}
         */
        Object produceResponse(long timestamp, T request);
    }

    /**
     * Handles the incoming response for an outgoing request.
     * @param <T> message type
     */
    public interface OutgoingRequestHandler<T> {

        /**
         * Indicates that a request has completed.
         * @param request original request message
         * @param response response message
         * @throws NullPointerException if any argument is {@code null}
         */
        void responseComplete(T request, Object response);

        /**
         * Indicates that a request has timed out and is giving up.
         * @param request original request message
         * @throws NullPointerException if any argument is {@code null}
         */
        void responseTimedOut(T request);
    }
}
