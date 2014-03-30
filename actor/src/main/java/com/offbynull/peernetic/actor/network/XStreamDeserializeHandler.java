package com.offbynull.peernetic.actor.network;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.binary.BinaryStreamDriver;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.commons.lang3.Validate;

public final class XStreamDeserializeHandler extends MessageToMessageDecoder<Object> {

    private XStream xstream;

    public XStreamDeserializeHandler() {
        this(new XStream(new BinaryStreamDriver()));
    }

    public XStreamDeserializeHandler(XStream xstream) {
        Validate.notNull(xstream);
        this.xstream = xstream;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, Object message, List<Object> out) throws Exception {
        Channel channel = ctx.channel();
        
        if (channel instanceof SocketChannel &&  message instanceof ByteBuf) {
            ByteBuf in = (ByteBuf) message;

            Object obj = convert(in);
            out.add(obj);
            
            return;
        } else if (channel instanceof DatagramChannel && message instanceof DatagramPacket) {
            DatagramPacket dp = (DatagramPacket) message;
            ByteBuf in = dp.content();

            Object obj = convert(in);
            out.add(new DefaultAddressedEnvelope<>(obj, dp.recipient(), dp.sender()));
            
            return;
        }
        
        throw new IllegalArgumentException();
    }
    
    private Object convert(ByteBuf in) {
        try {
            byte[] bytes = new byte[in.readableBytes()];
            in.readBytes(bytes);

            return xstream.fromXML(new ByteArrayInputStream(bytes));
        } finally {
            in.release();
        }
    }
}
