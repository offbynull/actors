// BASED OFF OF NETTY'S LocalEcho example

package com.offbynull.peernetic.nettyp2p.simulation;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;


public class LocalEchoServerHandler extends ChannelHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Write back as received
        DatagramPacket in = (DatagramPacket) msg;
        DatagramPacket out = new DatagramPacket(in.content(), in.sender());
        ctx.write(out);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}