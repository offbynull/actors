package com.offbynull.peernetic.networktools.netty.builders;

import com.offbynull.peernetic.networktools.netty.handlers.readwrite.Message;
import com.offbynull.peernetic.networktools.netty.handlers.readwrite.IncomingMessageListener;
import com.offbynull.peernetic.networktools.netty.handlers.readwrite.ReadToListenerHandler;
import com.offbynull.peernetic.networktools.netty.handlers.readwrite.ReadToQueueHandler;
import com.offbynull.peernetic.networktools.netty.handlers.readwrite.WriteFromQueueHandler;
import com.offbynull.peernetic.networktools.netty.handlers.selfblock.SelfBlockId;
import com.offbynull.peernetic.networktools.netty.handlers.selfblock.SelfBlockIdCheckHandler;
import com.offbynull.peernetic.networktools.netty.handlers.selfblock.SelfBlockIdPrependHandler;
import com.offbynull.peernetic.networktools.netty.handlers.xstream.XStreamDecodeHandler;
import com.offbynull.peernetic.networktools.netty.handlers.xstream.XStreamEncodeHandler;
import com.offbynull.peernetic.networktools.netty.channels.simulatedpacket.SimulatedPacketChannel;
import com.offbynull.peernetic.networktools.netty.channels.simulatedpacket.TransitPacketRepository;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.binary.BinaryStreamDriver;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ChannelFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import org.apache.commons.lang3.Validate;

public final class NettyClientBuilder {

    private SocketAddress localAddress;
    private SocketAddress remoteAddress;
    private TransitPacketRepository transitPacketRepository;
    private Type type;
    private SelfBlockId checkSelfBlockId;
    private SelfBlockId includeSelfBlockId;
    private Codec encodeHandler;
    private Codec decodeHandler;
    private List<WriteFromQueueHandlerSpec> writeHandlers;
    private List<ChannelHandler> customHandlers;
    private List<ChannelHandler> readHandlers;
    private EventLoopGroup eventLoopGroup;

    public NettyClientBuilder() {
        type = Type.UDP;
        encodeHandler = Codec.XSTREAM_BINARY;
        decodeHandler = Codec.XSTREAM_BINARY;
        localAddress = new InetSocketAddress(9000);
        writeHandlers = new ArrayList<>();
        customHandlers = new ArrayList<>();
        readHandlers = new ArrayList<>();
    }

    public NettyClientBuilder tcp(SocketAddress localAddress, SocketAddress remoteAddress) {
        Validate.notNull(localAddress);
        Validate.notNull(remoteAddress);

        type = Type.TCP;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.transitPacketRepository = null;
        return this;
    }

    public NettyClientBuilder udp(SocketAddress localAddress) {
        Validate.notNull(localAddress);

        type = Type.UDP;
        this.localAddress = localAddress;
        this.remoteAddress = null;
        this.transitPacketRepository = null;
        return this;
    }

    public NettyClientBuilder simulatedUdp(SocketAddress localAddress, TransitPacketRepository packetRepository) {
        Validate.notNull(localAddress);
        Validate.notNull(packetRepository);
        type = Type.SIMULATED_UDP;
        this.localAddress = localAddress;
        this.remoteAddress = null;
        this.transitPacketRepository = packetRepository;
        return this;
    }

    public NettyClientBuilder eventLoopGroup(EventLoopGroup eventLoopGroup) {
        Validate.notNull(eventLoopGroup);
        this.eventLoopGroup = eventLoopGroup;
        return this;
    }

    public NettyClientBuilder selfBlockId(SelfBlockId selfBlockId) {
        return checkForSelfBlockId(selfBlockId).includeSelfBlockId(selfBlockId);
    }

    public NettyClientBuilder checkForSelfBlockId(SelfBlockId selfBlockId) {
        Validate.notNull(selfBlockId);
        this.checkSelfBlockId = selfBlockId;
        return this;
    }

    public NettyClientBuilder includeSelfBlockId(SelfBlockId selfBlockId) {
        Validate.notNull(selfBlockId);
        this.includeSelfBlockId = selfBlockId;
        return this;
    }
    
    public NettyClientBuilder customHandlers(ChannelHandler... customHandlers) {
        Validate.noNullElements(customHandlers);
        this.customHandlers = new ArrayList<>(Arrays.asList(customHandlers));
        return this;
    }

    public NettyClientBuilder addReadDestination(BlockingQueue<Message> queue) {
        Validate.notNull(queue);
        readHandlers.add(new ReadToQueueHandler(queue));
        return this;
    }

    public NettyClientBuilder addReadDestination(IncomingMessageListener listener) {
        Validate.notNull(listener);
        readHandlers.add(new ReadToListenerHandler(listener));
        return this;
    }
    
    public NettyClientBuilder addWriteSource(BlockingQueue<Message> queue, long pollRate) {
        Validate.notNull(queue);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, pollRate);
        writeHandlers.add(new WriteFromQueueHandlerSpec(queue, pollRate));
        return this;
    }
    
    public NettyClientBuilder codec(Codec codec) {
        return encoder(codec).decoder(codec);
    }
    
    public NettyClientBuilder encoder(Codec codec) {
        Validate.notNull(codec);
        encodeHandler = codec;
        return this;
    }
    
    public NettyClientBuilder decoder(Codec codec) {
        Validate.notNull(codec);
        decodeHandler = codec;
        return this;
    }

    public NettyClient build() throws InterruptedException {
        List<ChannelHandler> combinedHandlers = new ArrayList<>();
        
        for (WriteFromQueueHandlerSpec wfqhs : writeHandlers) {
            combinedHandlers.add(new WriteFromQueueHandler(wfqhs.getQueue(), wfqhs.getPollRate(),
                    type != Type.TCP));
        }

        if (checkSelfBlockId != null) {
            combinedHandlers.add(new SelfBlockIdCheckHandler(checkSelfBlockId, false));
        }
        
        if (includeSelfBlockId != null) {
            combinedHandlers.add(new SelfBlockIdPrependHandler(includeSelfBlockId));
        }
        
        combinedHandlers.addAll(customHandlers);

        switch (encodeHandler) {
            case NONE:
                break;
            case XSTREAM_BINARY:
                combinedHandlers.add(new XStreamEncodeHandler(new XStream(new BinaryStreamDriver())));
                break;
            case XSTREAM_XML:
                combinedHandlers.add(new XStreamEncodeHandler(new XStream()));
                break;
            default:
                throw new IllegalStateException();
        }

        switch (decodeHandler) {
            case NONE:
                break;
            case XSTREAM_BINARY:
                combinedHandlers.add(new XStreamDecodeHandler(new XStream(new BinaryStreamDriver())));
                break;
            case XSTREAM_XML:
                combinedHandlers.add(new XStreamDecodeHandler(new XStream()));
                break;
            default:
                throw new IllegalStateException();
        }
        
        combinedHandlers.addAll(readHandlers);

        final ChannelHandler[] allHandlers = combinedHandlers.toArray(new ChannelHandler[0]);

        if (type == Type.TCP) {
            EventLoopGroup clientGroup = eventLoopGroup == null ? new NioEventLoopGroup(1) : eventLoopGroup;
            try {
                Bootstrap cb = new Bootstrap();
                cb.group(clientGroup);
                cb.channel(NioSocketChannel.class);
                cb.option(ChannelOption.SO_KEEPALIVE, true);
                cb.handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(allHandlers);
                    }
                });

                return new NettyClient(cb.connect(remoteAddress).sync().channel(), clientGroup);
            } catch (InterruptedException ie) {
                clientGroup.shutdownGracefully();
                throw ie;
            } catch (Exception e) {
                clientGroup.shutdownGracefully();
                throw new IllegalStateException("Failed to build Channel", e);
            }
        } else if (type == Type.UDP) {
            EventLoopGroup clientGroup = eventLoopGroup == null ? new NioEventLoopGroup(1) : eventLoopGroup;
            try {
                Bootstrap cb = new Bootstrap();
                cb.group(clientGroup)
                        .channel(NioDatagramChannel.class)
                        .handler(new ChannelInitializer<NioDatagramChannel>() {
                            @Override
                            public void initChannel(NioDatagramChannel ch) throws Exception {
                                ch.pipeline().addLast(allHandlers);
                            }
                        });
                return new NettyClient(cb.bind(localAddress).sync().channel(), clientGroup);
            } catch (InterruptedException ie) {
                clientGroup.shutdownGracefully();
                throw ie;
            } catch (Exception e) {
                clientGroup.shutdownGracefully();
                throw new IllegalStateException("Failed to build Channel", e);
            }
        } else if (type == Type.SIMULATED_UDP) {
            EventLoopGroup clientGroup = eventLoopGroup == null ? new DefaultEventLoopGroup() : eventLoopGroup;
            try {
                Bootstrap cb = new Bootstrap();
                cb.group(clientGroup)
                        .channelFactory(new ChannelFactory<SimulatedPacketChannel>() {
                            @Override
                            public SimulatedPacketChannel newChannel(EventLoop eventLoop) {
                                return new SimulatedPacketChannel(eventLoop, transitPacketRepository);
                            }
                        })
                        .handler(new ChannelInitializer<SimulatedPacketChannel>() {
                            @Override
                            public void initChannel(SimulatedPacketChannel ch) throws Exception {
                                ch.pipeline().addLast(allHandlers);
                            }
                        });
                return new NettyClient(cb.bind(localAddress).sync().channel(), clientGroup);
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

    public enum Codec {

        NONE,
        XSTREAM_XML,
        XSTREAM_BINARY
    }

    private static final class WriteFromQueueHandlerSpec {
        private BlockingQueue<Message> queue;
        private long pollRate;

        public WriteFromQueueHandlerSpec(BlockingQueue<Message> queue, long pollRate) {
            this.queue = queue;
            this.pollRate = pollRate;
        }

        public BlockingQueue<Message> getQueue() {
            return queue;
        }

        public long getPollRate() {
            return pollRate;
        }
        
    }
}
