// BASED OFF OF NETTY'S LocalEcho example

package com.offbynull.peernetic.nettysimulation;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

public class LocalEchoClientHandler extends SimpleChannelInboundHandler<Object> {

    @Override
    public void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Print as received
        DatagramPacket in = (DatagramPacket) msg;
        byte[] data = new byte[in.content().readableBytes()];
        in.content().readBytes(data);
        System.out.println(new String(data));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}