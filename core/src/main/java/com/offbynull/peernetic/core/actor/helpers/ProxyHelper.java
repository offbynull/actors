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

import com.offbynull.peernetic.core.actor.Actor;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.Address;
import org.apache.commons.lang3.Validate;

/**
 * A helper class for {@link Actor}s that act as a proxy. Usage example:
 * <pre>
 * Context ctx = (Context) cont.getContext();
 *
 * StartUnreliableProxy startMsg = ctx.getIncomingMessage();
 *
 * Line line = startMsg.getLine();
 * String timerPrefix = startMsg.getTimerPrefix();
 * String actorPrefix = startMsg.getActorPrefix();
 *
 * ProxyHelper proxyHelper = new ProxyHelper(ctx, actorPrefix);
 *
 * while (true) {
 *     cont.suspend();
 *     Object msg = ctx.getIncomingMessage();
 *     Instant time = ctx.getTime();
 *
 *     if (proxyHelper.isMessageFromActor()) {
 *         // This is an outgoing message from the actor to the outside
 * 
 *         ... potentially do some processing here ...
 * 
 *         // Foward message to outside
 *         ForwardInformation forwardInfo = proxyHelper.generateOutboundForwardInformation();
 *         ctx.addOutgoingMessage(
 *                 forwardInfo.getProxyFromId(),
 *                 forwardInfo.getProxyToAddress(),
 *                 ctx.getIncomingMessage());
 *     } else {
 *         // This is an incoming message from the outside to the actor
 *         ForwardInformation forwardInfo = proxyHelper.generatInboundForwardInformation();
 * 
 *         ... potentially do some processing here ...
 * 
 *         ctx.addOutgoingMessage(
 *                 forwardInfo.getProxyFromId(),
 *                 forwardInfo.getProxyToAddress(),
 *                 ctx.getIncomingMessage());
 *     }
 * }
 * </pre>
 * @author Kasra Faghihi
 */
public final class ProxyHelper {

    private final Context context;
    private final Address actorPrefix;

    /**
     * Construct a {@link ProxyHelper} instance.
     * @param context actor context
     * @param actorPrefix address of the actor being proxied
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code actorPrefix} is empty
     */
    public ProxyHelper(Context context, Address actorPrefix) {
        Validate.notNull(context);
        Validate.notNull(actorPrefix);
        Validate.isTrue(!actorPrefix.isEmpty());
        this.context = context;
        this.actorPrefix = actorPrefix;
    }

    /**
     * Generate forwarding information from the actor being proxied to the outside.
     * <p>
     * For example, imagine that "local:mainactor" (the actor being proxied) wants to send an outgoing message to "remote:actor2" (the
     * outside actor):
     * <ul>
     * <li>"local:mainactor" sends an outgoing message from "local:mainactor:child1" to "local:proxy:remote:actor2:child"</li>
     * <li>"local:proxy" receives the message and sends an outgoing message from "local:proxy:child1" to "remote:actor2:child"</li>
     * <li>"remote:actor2" receives the message from "local:proxy:child1" to "remote:actor2:child"</li>
     * </ul>
     * Note how the address suffixes stay intact. If "remote:actor2" wanted to reply, it could do so by sending a message back to
     * "local:proxy:child". The proxy should forward that message to "local:mainactor:child".
     * @throws IllegalStateException if the incoming message's destination address doesn't contain any forwarding information
     * @return forwarding information
     */
    public ForwardInformation generateOutboundForwardInformation() {
        // Get address to proxy to
        Address selfAddr = context.getSelf();
        Address dstAddr = context.getDestination();
        Address proxyToAddress = dstAddr.removePrefix(selfAddr); // treat suffix for dst of this msg as address to proxy to
        Validate.validState(!proxyToAddress.isEmpty());

        // Get suffix for from address
        Address srcAddr = context.getSource();
        Address proxyFromId = srcAddr.removePrefix(actorPrefix);
        
        return new ForwardInformation(proxyFromId, proxyToAddress);
    }

    /**
     * Generate forwarding information from the outside to the actor being proxied.
     * <p>
     * For example, imagine that "remote:actor2" (the outside actor) wants to send an outgoing message to "local:mainactor" (the actor
     * being proxied):
     * <ul>
     * <li>"remote:actor2" sends an outgoing message from "remote:actor2:child" to "local:proxy:child1"</li>
     * <li>"local:proxy" receives the message and sends an outgoing message from "local:proxy:remote:actor2:child" to
     * "local:mainactor:child1"</li>
     * <li>"local:mainactor" receives the message from "local:proxy:remote:actor2:child" to "local:mainactor:child1"</li>
     * </ul>
     * Note how the address suffixes stay intact. If "local:mainactor" wanted to reply, it could do so by sending a message back to
     * "local:proxy:remote:actor2:child". The proxy should forward that message to "remote:actor2:child".
     * @return forwarding information
     */
    public ForwardInformation generatInboundForwardInformation() {
        // Get suffix portion of incoming message's destination address
        Address selfAddr = context.getSelf();
        Address dstAddr = context.getDestination();
        Address suffix = dstAddr.removePrefix(selfAddr);
        Address proxyToAddress = actorPrefix.appendSuffix(suffix);
        
        // Get sender
        Address proxyFromId = context.getSource();
        
        return new ForwardInformation(proxyFromId, proxyToAddress);
    }
    
//    public void forwardToOutside(Object message) {
//        Validate.notNull(message);
//        
//        ForwardInformation forwardInfo = generateOutboundForwardInformation();
//
//        context.addOutgoingMessage(
//                forwardInfo.getProxyFromId(),
//                forwardInfo.getProxyToAddress(),
//                message);
//    }
//
//    public void forwardToActor(Object message) {
//        Validate.notNull(message);
//        
//        ForwardInformation forwardInfo = generatInboundForwardInformation();
//
//        context.addOutgoingMessage(
//                forwardInfo.getProxyFromId(),
//                forwardInfo.getProxyToAddress(),
//                message);
//    }
  
    /**
     * Determine if the incoming message is from the actor being proxied.
     * @return {@code true} if the incoming message is from the actor being proxied, {@code false} otherwise
     */
    public boolean isMessageFromActor() {
        return isMessageFrom(actorPrefix);
    }
    
    /**
     * Determines if the incoming message is from some address prefix. Convenience method equivalent to calling
     * {@code addressPrefix.isPrefixOf(context.getSource())}.
     * @param addressPrefix address prefix
     * @throws NullPointerException if any argument is {@code null}
     * @return {@code true} if the incoming message is from the specified address prefix, {@code false} otherwise
     */
    public boolean isMessageFrom(Address addressPrefix) {
        Validate.notNull(addressPrefix);
        return addressPrefix.isPrefixOf(context.getSource());
    }
    
    /**
     * Forwarding information.
     */
    public static final class ForwardInformation {
        private final Address proxyFromId;
        private final Address proxyToAddress;
        
        private ForwardInformation(Address proxyFromId, Address proxyToAddress) {
            this.proxyFromId = proxyFromId;
            this.proxyToAddress = proxyToAddress;
        }

        /**
         * Get the id (address suffix) to forward from.
         * @return id (address suffix) to forward from
         */
        public Address getProxyFromId() {
            return proxyFromId;
        }

        /**
         * Get the address to forward to.
         * @return address to forward to
         */
        public Address getProxyToAddress() {
            return proxyToAddress;
        }
        
    }
}
