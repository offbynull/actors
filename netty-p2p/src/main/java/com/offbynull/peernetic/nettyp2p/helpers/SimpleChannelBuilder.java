package com.offbynull.peernetic.nettyp2p.helpers;

import com.offbynull.peernetic.nettyp2p.handlers.read.IncomingMessage;
import com.offbynull.peernetic.nettyp2p.handlers.read.IncomingMessageListener;
import com.offbynull.peernetic.nettyp2p.handlers.read.ReadToListenerHandler;
import com.offbynull.peernetic.nettyp2p.handlers.read.ReadToQueueHandler;
import com.offbynull.peernetic.nettyp2p.simulation.LocalDatagramChannel;
import com.offbynull.peernetic.nettyp2p.simulation.TransitPacketRepository;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ChannelFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import org.apache.commons.lang3.Validate;

public final class SimpleChannelBuilder {

    private SocketAddress localAddress;
    private SocketAddress remoteAddress;
    private TransitPacketRepository transitPacketRepository;
    private Type type;
    private ChannelHandler[] handlers;
    private List<ChannelHandler> tailHandlers;

    public SimpleChannelBuilder() {
        type = Type.UDP;
        localAddress = new InetSocketAddress(9000);
        tailHandlers = new ArrayList<>();
    }

    public SimpleChannelBuilder tcp(SocketAddress localAddress, SocketAddress remoteAddress) {
        Validate.notNull(localAddress);
        Validate.notNull(remoteAddress);
        
        type = Type.TCP;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.transitPacketRepository = null;
        return this;
    }

    public SimpleChannelBuilder udp(SocketAddress localAddress) {
        Validate.notNull(localAddress);
        
        type = Type.UDP;
        this.localAddress = localAddress;
        this.remoteAddress = null;
        this.transitPacketRepository = null;
        return this;
    }

    public SimpleChannelBuilder simulatedUdp(SocketAddress localAddress, TransitPacketRepository packetRepository) {
        Validate.notNull(localAddress);
        Validate.notNull(packetRepository);
        type = Type.SIMULATED_UDP;
        this.localAddress = localAddress;
        this.remoteAddress = null;
        this.transitPacketRepository = packetRepository;
        return this;
    }

    public SimpleChannelBuilder handlers(ChannelHandler... handlers) {
        Validate.noNullElements(handlers);
        this.handlers = Arrays.copyOf(handlers, handlers.length);
        return this;
    }
    
    public SimpleChannelBuilder funnelReadsToQueue(BlockingQueue<IncomingMessage> queue) {
        Validate.notNull(queue);
        tailHandlers.add(new ReadToQueueHandler(queue));
        return this;
    }

    public SimpleChannelBuilder funnelReadsToListener(IncomingMessageListener listener) {
        Validate.notNull(listener);
        tailHandlers.add(new ReadToListenerHandler(listener));
        return this;
    }

    public SimpleChannel build() throws InterruptedException {
        List<ChannelHandler> combinedHandlers = new ArrayList<>();
        combinedHandlers.addAll(Arrays.asList(handlers));
        combinedHandlers.addAll(tailHandlers);
        final ChannelHandler[] allHandlers = combinedHandlers.toArray(new ChannelHandler[0]);
        
        if (type == Type.TCP) {
            EventLoopGroup clientGroup = new NioEventLoopGroup(1);
            try {
                Bootstrap cb = new Bootstrap(); // (1)
                cb.group(clientGroup); // (2)
                cb.channel(NioSocketChannel.class); // (3)
                cb.option(ChannelOption.SO_KEEPALIVE, true); // (4)
                cb.handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(allHandlers);
                    }
                });

                return new SimpleChannel(cb.connect(remoteAddress).sync().channel(), clientGroup);
            } catch (InterruptedException ie) {
                clientGroup.shutdownGracefully();
                throw ie;
            } catch (Exception e) {
                clientGroup.shutdownGracefully();
                throw new IllegalStateException("Failed to build Channel", e);
            }
        } else if (type == Type.UDP) {
            EventLoopGroup clientGroup = new NioEventLoopGroup(1);
            try {
                Bootstrap cb = new Bootstrap();
                cb.group(clientGroup)
                        .channel(DatagramChannel.class)
                        .handler(new ChannelInitializer<LocalDatagramChannel>() {
                            @Override
                            public void initChannel(LocalDatagramChannel ch) throws Exception {
                                ch.pipeline().addLast(allHandlers);
                            }
                        });
                return new SimpleChannel(cb.bind(localAddress).sync().channel(), clientGroup);
            } catch (InterruptedException ie) {
                clientGroup.shutdownGracefully();
                throw ie;
            } catch (Exception e) {
                clientGroup.shutdownGracefully();
                throw new IllegalStateException("Failed to build Channel", e);
            }
        } else if (type == Type.SIMULATED_UDP) {
            EventLoopGroup clientGroup = new NioEventLoopGroup(1);
            try {
                Bootstrap cb = new Bootstrap();
                cb.group(clientGroup)
                        .channelFactory(new ChannelFactory<LocalDatagramChannel>() {
                            @Override
                            public LocalDatagramChannel newChannel(EventLoop eventLoop) {
                                return new LocalDatagramChannel(eventLoop, transitPacketRepository);
                            }
                        })
                        .handler(new ChannelInitializer<LocalDatagramChannel>() {
                            @Override
                            public void initChannel(LocalDatagramChannel ch) throws Exception {
                                ch.pipeline().addLast(allHandlers);
                            }
                        });
                return new SimpleChannel(cb.bind(localAddress).sync().channel(), clientGroup);
            } catch (InterruptedException ie) {
                clientGroup.shutdownGracefully();
                throw ie;
            } catch (Exception e) {
                clientGroup.shutdownGracefully();
                throw new IllegalStateException("Failed to build Channel", e);
            }
        } else {
            throw new IllegalStateException();
        }
    }

    private enum Type {

        UDP,
        TCP,
        SIMULATED_UDP,
    }
    
    public static final class SimpleChannel implements Closeable {
        private Channel channel;
        private EventLoopGroup eventLoopGroup;

        private SimpleChannel(Channel channel, EventLoopGroup eventLoopGroup) {
            this.channel = channel;
            this.eventLoopGroup = eventLoopGroup;
        }

        public ChannelFuture write(Object msg) {
            return channel.write(msg);
        }

        public ChannelFuture write(Object msg, ChannelPromise promise) {
            return channel.write(msg, promise);
        }

        public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
            return channel.writeAndFlush(msg, promise);
        }

        public ChannelFuture writeAndFlush(Object msg) {
            return channel.writeAndFlush(msg);
        }

        @Override
        public void close() {
            try {
                channel.close().sync();
            } catch (Exception ex) {
                // do nothing
            }
            
            try {
                eventLoopGroup.shutdownGracefully().get();
            } catch (Exception ex) {
                // do nothing
            }
        }
        
    }
}
