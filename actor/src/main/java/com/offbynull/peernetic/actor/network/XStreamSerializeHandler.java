package com.offbynull.peernetic.actor.network;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.binary.BinaryStreamDriver;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import org.apache.commons.lang3.Validate;

public final class XStreamSerializeHandler extends MessageToMessageEncoder<Object> {

    private XStream xstream;

    public XStreamSerializeHandler() {
        this(new XStream(new BinaryStreamDriver()));
    }

    public XStreamSerializeHandler(XStream xstream) {
        Validate.notNull(xstream);
        this.xstream = xstream;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object message, List<Object> out) throws Exception {
        Channel channel = ctx.channel();
        
        if (channel instanceof DatagramChannel && message instanceof AddressedEnvelope) {
            AddressedEnvelope<Object, InetSocketAddress> env = (AddressedEnvelope<Object, InetSocketAddress>) message;
            ByteBuf buf = convert(env.content());
            out.add(new DatagramPacket(buf, env.recipient(), env.sender()));
            return;
        } else if (channel instanceof SocketChannel) {
            ByteBuf buf = convert(message);
            out.add(buf);
            return;
        }

        throw new IllegalArgumentException();
    }

    private ByteBuf convert(Object message) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xstream.toXML(message, baos);
        byte[] data = baos.toByteArray();
        return Unpooled.wrappedBuffer(data);
    }
}
