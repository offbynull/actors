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
package com.offbynull.peernetic.common.concurrent.actor;

import org.apache.commons.lang3.Validate;

/**
 * Encapsulates a message.
 * @author Kasra Faghihi
 */
public final class Message {
    private Object key;
    private Object responseKey;
    private ActorQueueWriter writer;
    private Object content;

    private Message() {
        // do nothing
    }

    /**
     * Get the contents of this message.
     * @return contents of this message
     */
    public Object getContent() {
        return content;
    }
    
    /**
     * Get the key for which this message is a response to.
     * @return key for which this message is a response to, or {@code null} if this message is not a response to another message
     */
    public Object getResponseToId() {
        return responseKey;
    }
    
    /**
     * Get the key for which this message is a response to only if it matches a certain type.
     * @param <T> type
     * @param type type to check against
     * @throws NullPointerException if any argument is {@code null}
     * @return ID for which this message is a response to, or {@code null} if this message is not a response to another message / doesn't
     * match the specified type
     */
    public <T> T getResponseToId(Class<T> type) {
        if (responseKey == null) {
            return null;
        }

        return responseKey.getClass() == type ? (T) responseKey : null;
    }
    
    /**
     * Get the key to use for responses to this message.
     * @return key to use for responses to this message, or {@code null} if the message doesn't expect a response
     */
    public MessageResponder getResponder() {
        return new MessageResponder();
    }
    
    public final class MessageResponder {
        private MessageResponder() {
            // do not allow outside parties to instantiate
        }
        
        /**
         * Get the ID expected by responses to this message.
         * @return ID expected by responses
         */
        public Object getId() {
            return key;
        }

        /**
         * Get the ID expected by responses to this message only if it matches a certain type.
         * @param <T> type
         * @param type type to check against
         * @return ID expected by responses, or {@code null} if it doesn't exist or doesn't match the specified type
         * @throws NullPointerException if any argument is {@code null}
         */
        public <T> T getId(Class<T> type) {
            if (key == null) {
                return null;
            }
            
            return key.getClass() == type ? (T) key : null;
        }
        
        /**
         * Gets the writer to feed responses for this message to.
         * @return writer to feed responses for this message to
         */
        public ActorQueueWriter getWriter() {
            return writer;
        }
        
        public void respondImmediately(Object content) {
            writer.push(Message.createResponseMessage(key, content));
        }
        
        public void respondDeferred(PushQueue queue, Object content) {
            queue.queueResponseMessage(writer, key, content);
        }
    }

    /**
     * Constructs a {@link Message} object that doesn't expect a response.
     * @param content message content
     * @throws NullPointerException if any argument is {@code null}
     * @return new message
     */
    public static Message createOneWayMessage(Object content) {
        Validate.notNull(content);
        
        Message message = new Message();
        
        message.content = content;
        
        return message;
    }

    /**
     * Constructs a {@link Message} object that can be responded to.
     * @param key ID unique to this message
     * @param content message content
     * @param writer writer that takes in responses for this message
     * @throws NullPointerException if any argument is {@code null}
     * @return new message
     */
    public static Message createRespondableMessage(Object key, ActorQueueWriter writer, Object content) {
        Validate.notNull(key);
        Validate.notNull(content);
        Validate.notNull(writer);
        
        Message message = new Message();
        
        message.key = key;
        message.content = content;
        message.writer = writer;
        
        return message;
    }

    /**
     * Constructs a {@link Message} object that is a response to another {@link Message}.
     * @param origKey ID of message being responded to
     * @param content message content
     * @throws NullPointerException if any argument is {@code null}
     * @return new message
     */
    public static Message createResponseMessage(Object origKey, Object content) {
        Validate.notNull(origKey);
        Validate.notNull(content);
        
        Message message = new Message();
        
        message.responseKey = origKey;
        message.content = content;
        
        return message;
    }
    
    /**
     * Constructs a {@link Message} object that is a response to another {@link Message} and can also be responded to.
     * @param key ID unique to this message
     * @param origKey ID of message being responded to
     * @param content message content
     * @param writer writer that takes in responses for this message
     * @throws NullPointerException if any argument is {@code null}
     * @return new message
     */
    public static Message createRespondableResponseMessage(Object key, ActorQueueWriter writer, Object origKey, Object content) {
        Validate.notNull(key);
        Validate.notNull(origKey);
        Validate.notNull(content);
        Validate.notNull(writer);
        
        Message message = new Message();
        
        message.key = key;
        message.responseKey = origKey;
        message.content = content;
        message.writer = writer;
        
        return message;
    }
}
