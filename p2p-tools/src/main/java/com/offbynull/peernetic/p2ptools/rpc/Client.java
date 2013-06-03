package com.offbynull.peernetic.p2ptools.rpc;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import java.net.InetSocketAddress;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicReference;

public final class Client {

    AtomicReference<Object> returnValue = new AtomicReference<>();

    public Object transmitRpcCall(InetSocketAddress bootstrapAddress,
            final InvokeData data) throws InterruptedException {

        if (bootstrapAddress == null) {
            throw new NullPointerException();
        }

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            final SynchronousQueue<Object> resQueue = new SynchronousQueue<>();

            Bootstrap b = new Bootstrap();
            ChannelInitializer<SocketChannel> chanInit =
                    new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new ObjectEncoder(),
                                    new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                    new ClientHandler(resQueue, data));
                        }
                    };

            b.group(group).channel(NioSocketChannel.class).handler(chanInit);

            // Start the connection attempt.
            b.connect(bootstrapAddress).sync();//.channel().closeFuture().sync();

            return resQueue.take();
        } finally {
            group.shutdownGracefully();
        }
    }

    private static final class ClientHandler
            extends ChannelInboundMessageHandlerAdapter<Object> {

        private SynchronousQueue<Object> resQueue;
        private InvokeData object;

        public ClientHandler(SynchronousQueue<Object> resQueue,
                InvokeData object) {
            this.resQueue = resQueue;
            this.object = object;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            ctx.write(object);
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, Object resp)
                throws Exception {
            resQueue.put(resp);
            ctx.close();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                throws Exception {
            ctx.close();
        }
    }
}