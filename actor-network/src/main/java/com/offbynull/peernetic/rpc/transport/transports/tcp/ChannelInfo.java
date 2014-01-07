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
package com.offbynull.peernetic.rpc.transport.transports.tcp;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import org.apache.commons.lang3.Validate;

abstract class ChannelInfo {

    private SocketChannel channel;
    private StreamIoBuffers buffers;
    private SelectionKey selectionKey;

    public ChannelInfo(SocketChannel channel, StreamIoBuffers buffers, SelectionKey selectionKey) {
        Validate.notNull(channel);
        Validate.notNull(buffers);
        Validate.notNull(selectionKey);

        this.channel = channel;
        this.buffers = buffers;
        this.selectionKey = selectionKey;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public StreamIoBuffers getBuffers() {
        return buffers;
    }

    public SelectionKey getSelectionKey() {
        return selectionKey;
    }

}