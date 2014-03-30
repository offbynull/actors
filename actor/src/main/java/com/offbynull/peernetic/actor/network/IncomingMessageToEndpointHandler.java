package com.offbynull.peernetic.actor.network;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.Outgoing;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import java.net.InetSocketAddress;
import org.apache.commons.lang3.Validate;

public final class IncomingMessageToEndpointHandler extends ChannelHandlerAdapter {
    
    private Endpoint nettyEndpoint;
    private Endpoint dstEndpoint;

    public IncomingMessageToEndpointHandler(Endpoint nettyEndpoint, Endpoint dstEndpoint) {
        Validate.notNull(nettyEndpoint);
        Validate.notNull(dstEndpoint);
        this.nettyEndpoint = nettyEndpoint;
        this.dstEndpoint = dstEndpoint;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel channel = ctx.channel();
        
        if (channel instanceof DatagramChannel && msg instanceof AddressedEnvelope) {
            AddressedEnvelope<Object, InetSocketAddress> env = (AddressedEnvelope<Object, InetSocketAddress>) msg;
            InetSocketAddress from = env.sender();
            Object content = env.content();

            NetworkEndpoint<InetSocketAddress> srcEndpoint = new NetworkEndpoint<>(channel, from);
            
            Outgoing outgoing = new Outgoing(content, dstEndpoint);
            dstEndpoint.push(srcEndpoint, outgoing);
            
            return;
        } else if (channel instanceof SocketChannel) {
            InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
            
            NetworkEndpoint<InetSocketAddress> srcEndpoint = new NetworkEndpoint<>(channel, remoteAddress);
            
            Outgoing outgoing = new Outgoing(msg, dstEndpoint);
            dstEndpoint.push(srcEndpoint, outgoing);
            
            return;
        }
        
        throw new IllegalArgumentException();
    }
}
