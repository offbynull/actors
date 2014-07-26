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
package com.offbynull.peernetic.debug.testnetwork;

import com.offbynull.peernetic.debug.testnetwork.messages.ArriveMessage;
import com.offbynull.peernetic.debug.testnetwork.messages.DepartMessage;
import com.offbynull.peernetic.debug.testnetwork.messages.TransitMessage;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Collection;
import org.apache.commons.lang3.Validate;

/**
 * Controls how a test network behaves. For example, depending on the line, a message may be dropped/duplicated/corrupted/slow/fast/etc...
 *
 * @author Kasra Faghihi
 * @param <A> address type
 */
public interface Line<A> {

    /**
     * Called when a message is put on the network.
     *
     * @param time time of departure
     * @param departMessage outgoing message
     * @return list of {@link TransitMessage} objects generated from {@code departMessage}
     */
    Collection<TransitMessage<A>> depart(Instant time, BufferMessage<A> departMessage);

    /**
     * Called when a message on the network reaches its destination.
     *
     * @param time time of arrival
     * @param transitMessage message that has arrived
     * @return list of {@link ArriveMessage} objects generated from {@code transitMessage}
     */
    Collection<BufferMessage<A>> arrive(Instant time, TransitMessage<A> transitMessage);

    public static final class BufferMessage<A> {

        private ByteBuffer data;
        private A source;
        private A destination;

        public BufferMessage(ByteBuffer data, A source, A destination) {
            Validate.notNull(data);
            Validate.notNull(source);
            Validate.notNull(destination);
            this.data = ByteBufferUtils.copyContents(data);
            this.source = source;
            this.destination = destination;
        }

        public ByteBuffer getData() {
            return data.asReadOnlyBuffer();
        }

        public void setData(ByteBuffer data) {
            this.data = data;
        }

        public A getSource() {
            return source;
        }

        public void setSource(A source) {
            this.source = source;
        }

        public A getDestination() {
            return destination;
        }

        public void setDestination(A destination) {
            this.destination = destination;
        }

    }
}
