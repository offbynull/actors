/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.network.gateways.udp;

import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.shuttle.Message;
import io.netty.channel.Channel;
import io.netty.channel.DefaultAddressedEnvelope;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InternalShuttle implements Shuttle {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalShuttle.class);
    
    private String prefix;
    private Channel channel;

    InternalShuttle(String prefix, Channel channel) {
        Validate.notNull(prefix);
        Validate.notEmpty(prefix);
        Validate.notNull(channel);

        this.prefix = prefix;
        this.channel = channel;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void send(Collection<Message> messages) {
        Validate.notNull(messages);
        Validate.noNullElements(messages);

        messages.forEach(x -> {
            try {
                String dst = x.getDestinationAddress();
                String[] splitDst = AddressUtils.splitAddress(dst);
                
                Validate.isTrue(splitDst.length >= 2);
                String dstPrefix = splitDst[0];
                String dstAddress = splitDst[1];
                
                Validate.isTrue(dstPrefix.equals(prefix));
                
                String dstSuffix = null;
                if (splitDst.length > 2) {
                    dstSuffix = AddressUtils.getAddress(2, splitDst);
                }

                InetSocketAddress dstAddr = fromShuttleAddress(dstAddress);
                EncapsulatedMessage em = new EncapsulatedMessage(dstSuffix, x.getMessage());
                DefaultAddressedEnvelope<Object, InetSocketAddress> datagramPacket = new DefaultAddressedEnvelope<>(em, dstAddr);
                
                channel.writeAndFlush(datagramPacket);
            } catch (Exception e) {
                LOGGER.error("Error shuttling message: " + x, e);
            }
        });
    }

    private static InetSocketAddress fromShuttleAddress(String address) throws UnknownHostException, DecoderException {
        int splitIdx = address.indexOf('.');
        Validate.isTrue(splitIdx != -1);

        String addrStr = address.substring(0, splitIdx);
        String portStr = address.substring(splitIdx + 1);

        InetAddress addr = InetAddress.getByAddress(Hex.decodeHex(addrStr.toCharArray()));
        int port = Integer.parseInt(portStr);

        return new InetSocketAddress(addr, port);
    }
}
