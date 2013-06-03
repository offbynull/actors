package com.offbynull.peernetic.p2ptools.rpc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.apache.commons.lang3.reflect.MethodUtils;

public final class Server {

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private State state;

    public Server() {
        state = State.CREATED;
    }

    public void start(int port, final Object obj)
            throws InterruptedException {
        if (state != State.CREATED) {
            throw new IllegalStateException();
        }

        if (obj == null) {
            throw new NullPointerException();
        }

        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast("encoder", new ObjectEncoder());
                    ch.pipeline().addLast("decoder", new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
                    ch.pipeline().addLast("handler", new ServerHandler(obj));
                }
            });

            // Bind and start to accept incoming connections.
            b.bind(port).sync();

            state = State.STARTED;
        } catch(RuntimeException | InterruptedException ex) {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            
            throw ex;
        }
    }

    public void stop() throws InterruptedException {
        if (state != State.STARTED) {
            throw new IllegalStateException();
        }

        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();

        state = State.STOPPED;
    }

    private enum State {

        CREATED,
        STARTED,
        STOPPED
    }

    private static final class ServerHandler
            extends ChannelInboundMessageHandlerAdapter<InvokeData> {

        private Object object;

        public ServerHandler(Object object) {
            if (object == null) {
                throw new NullPointerException();
            }

            this.object = object;
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, InvokeData msg)
                throws Exception {
            Object resp = MethodUtils.invokeMethod(object, msg.getMethodName(),
                    msg.getArguments());
            ctx.write(resp);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                throws Exception {
            ctx.close();
        }
    }
}
