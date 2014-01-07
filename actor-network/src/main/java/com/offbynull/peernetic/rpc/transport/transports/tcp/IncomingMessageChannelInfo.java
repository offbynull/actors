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

final class IncomingMessageChannelInfo extends ChannelInfo {

    public IncomingMessageChannelInfo(SocketChannel channel, StreamIoBuffers buffers, SelectionKey selectionKey) {
        super(channel, buffers, selectionKey);
    }
    
}
