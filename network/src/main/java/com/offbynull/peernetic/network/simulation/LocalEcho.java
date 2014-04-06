// BASED OFF OF NETTY'S LocalEcho example

package com.offbynull.peernetic.network.simulation;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ChannelFactory;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramPacket;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import org.apache.commons.lang3.Range;

public class LocalEcho {

    public LocalEcho() {
    }

    public void run() throws Exception {
        final TransitPacketRepository repository = TransitPacketRepository.create(
                new RandomLine(12345L, Range.between(0.0, 0.0), Range.is(0.0), Range.is(50.0), Range.is(0.0)));

        EventLoopGroup clientGroup1 = new DefaultEventLoopGroup();
        EventLoopGroup clientGroup2 = new DefaultEventLoopGroup();
        try {
            Bootstrap cb1 = new Bootstrap();
            cb1.group(clientGroup1)
//                    .channel(LocalDatagramChannel.class)
                    .channelFactory(new ChannelFactory<LocalDatagramChannel>() {
                        @Override
                        public LocalDatagramChannel newChannel(EventLoop eventLoop) {
                            return new LocalDatagramChannel(eventLoop, repository);
                        }
                    })
                    .handler(new ChannelInitializer<LocalDatagramChannel>() {
                        @Override
                        public void initChannel(LocalDatagramChannel ch) throws Exception {
                            ch.pipeline().addLast(
//                                    new LoggingHandler(LogLevel.INFO),
                                    new LocalEchoClientHandler());
                        }
                    });

            Bootstrap cb2 = new Bootstrap();
            cb2.group(clientGroup2)
//                    .channel(LocalDatagramChannel.class)
                    .channelFactory(new ChannelFactory<LocalDatagramChannel>() {
                        @Override
                        public LocalDatagramChannel newChannel(EventLoop eventLoop) {
                            return new LocalDatagramChannel(eventLoop, repository);
                        }
                    })
                    .handler(new ChannelInitializer<LocalDatagramChannel>() {
                        @Override
                        public void initChannel(LocalDatagramChannel ch) throws Exception {
                            ch.pipeline().addLast(
//                                    new LoggingHandler(LogLevel.INFO),
                                    new LocalEchoServerHandler());
                        }
                    });

            InetSocketAddress addr1 = new InetSocketAddress("1.1.1.1", 1111);
            InetSocketAddress addr2 = new InetSocketAddress("2.2.2.2", 2222);
            Channel ch1 = cb1.bind(addr1).sync().channel();
            Channel ch2 = cb2.bind(addr2).sync().channel();

            // Read commands from the stdin.
            System.out.println("Enter text (quit to end)");
            ChannelFuture lastWriteFuture = null;
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            for (;;) {
                String line = in.readLine();
                if (line == null || "quit".equalsIgnoreCase(line)) {
                    break;
                }

                // Sends the received line to the server.
                lastWriteFuture = ch1.writeAndFlush(new DatagramPacket(Unpooled.wrappedBuffer(line.getBytes()), addr2));
            }

            // Wait until all messages are flushed before closing the channel.
            if (lastWriteFuture != null) {
                lastWriteFuture.awaitUninterruptibly();
            }
        } finally {
            clientGroup1.shutdownGracefully();
            clientGroup2.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        new LocalEcho().run();
    }
}
