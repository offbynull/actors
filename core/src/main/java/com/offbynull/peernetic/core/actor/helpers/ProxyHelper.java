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
package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import static com.offbynull.peernetic.core.shuttle.AddressUtils.SEPARATOR;
import org.apache.commons.lang3.Validate;

public final class ProxyHelper {

    private final Context context;
    private final String actorPrefix;

    public ProxyHelper(Context context, String actorPrefix) {
        Validate.notNull(context);
        Validate.notNull(actorPrefix);
        this.context = context;
        this.actorPrefix = actorPrefix;
    }

    public ForwardInformation generateOutboundForwardInformation() {
        // Get address to proxy to
        String selfAddr = context.getSelf();
        String dstAddr = context.getDestination();
        String proxyToAddress = AddressUtils.relativize(selfAddr, dstAddr); // treat suffix for dst of this msg as address to proxy to

        // Get suffix for from address
        String srcAddr = context.getSource();
        String proxyFromId = AddressUtils.relativize(actorPrefix, srcAddr);
        
        return new ForwardInformation(proxyFromId, proxyToAddress);
    }
    
    public ForwardInformation generatInboundForwardInformation() {
        // Get suffix portion of incoming message's destination address
        String selfAddr = context.getSelf();
        String dstAddr = context.getDestination();
        String suffix = AddressUtils.relativize(selfAddr, dstAddr);
        String proxyToAddress = actorPrefix + (suffix != null ? SEPARATOR + suffix : "");
        
        // Get sender
        String proxyFromId = context.getSource();
        
        return new ForwardInformation(proxyFromId, proxyToAddress);
    }
    
    public void forwardToOutside(Object message) {
        Validate.notNull(message);
        
        ForwardInformation forwardInfo = generateOutboundForwardInformation();

        context.addOutgoingMessage(
                forwardInfo.getProxyFromId(),
                forwardInfo.getProxyToAddress(),
                message);
    }

    public void forwardToActor(Object message) {
        Validate.notNull(message);
        
        ForwardInformation forwardInfo = generatInboundForwardInformation();

        context.addOutgoingMessage(
                forwardInfo.getProxyFromId(),
                forwardInfo.getProxyToAddress(),
                message);
    }
    
    public boolean isMessageFromActor() {
        return isMessageFrom(actorPrefix);
    }
    
    public boolean isMessageFrom(String addressPrefix) {
        Validate.notNull(addressPrefix);
        return AddressUtils.isPrefix(addressPrefix, context.getSource());
    }
    
    public static final class ForwardInformation {
        private final String proxyFromId;
        private final String proxyToAddress;

        private ForwardInformation(String proxyFromId, String proxyToAddress) {
            this.proxyFromId = proxyFromId;
            this.proxyToAddress = proxyToAddress;
        }

        public String getProxyFromId() {
            return proxyFromId;
        }

        public String getProxyToAddress() {
            return proxyToAddress;
        }
        
    }
}
