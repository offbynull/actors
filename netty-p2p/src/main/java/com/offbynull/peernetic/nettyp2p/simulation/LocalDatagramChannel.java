// BASED OFF OF NETTY'S LocalChannel

package com.offbynull.peernetic.nettyp2p.simulation;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.AbstractChannel;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.EventLoop;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.util.ReferenceCountUtil;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetBoundException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Queue;

/**
 * A {@link Channel} for simulated packet-based network (e.g. datagram) transport.
 */
public class LocalDatagramChannel extends AbstractChannel {

    private enum State {

        OPEN, BOUND, CLOSED
    }

    private static final ChannelMetadata METADATA = new ChannelMetadata(false);

    private final ChannelConfig config = new DefaultChannelConfig(this);
    private final Queue<Object> inboundBuffer = new ArrayDeque<>();
    private final Runnable readTask = new Runnable() {
        @Override
        public void run() {
            ChannelPipeline pipeline = pipeline();
            for (;;) {
                Object m = inboundBuffer.poll();
                if (m == null) {
                    break;
                }
                pipeline.fireChannelRead(m);
            }
            pipeline.fireChannelReadComplete();
        }
    };

    private volatile State state;
    private volatile TransitPacketRepository repository;
    private volatile SocketAddress localAddress;
    private volatile boolean readInProgress;

    public LocalDatagramChannel(EventLoop eventLoop, TransitPacketRepository repository) {
        super(null, eventLoop);
        this.repository = repository;
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return new LocalUnsafe();
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop instanceof SingleThreadEventLoop;
    }

    @Override
    protected SocketAddress localAddress0() {
        return localAddress;
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return null; // Do nothing -- this is a packet-based network with no connection abstraction
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        this.localAddress = (SocketAddress) localAddress;
        repository.registerChannel(this.localAddress, this);
        state = State.BOUND;
    }

    @Override
    protected void doDisconnect() throws Exception {
        // Do nothing -- this is a packet-based network with no connection abstraction
    }

    @Override
    protected void doClose() throws Exception {
        if (state != State.CLOSED) {
            // Update all internal state before the closeFuture is notified.
            repository.unregisterChannel(localAddress);
            if (localAddress != null) {
                localAddress = null;
            }
            state = State.CLOSED;
        }
    }

    @Override
    protected void doDeregister() throws Exception {
        // Do nothing
    }

    @Override
    protected void doBeginRead() throws Exception {
        if (readInProgress) {
            return;
        }

        if (inboundBuffer.isEmpty()) {
            readInProgress = true;
            return;
        }

        eventLoop().execute(readTask);
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        switch (state) {
            case OPEN:
                throw new NotYetBoundException();
            case CLOSED:
                throw new ClosedChannelException();
        }

        int size = in.size();
        for (int i = 0; i < size; i++) {
            Object msg = ReferenceCountUtil.retain(in.current());
            in.remove();

            if (msg instanceof AddressedEnvelope) {
                AddressedEnvelope<ByteBuf, SocketAddress> packet = (AddressedEnvelope<ByteBuf, SocketAddress>) msg;

                ByteBuf buf = packet.content();
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);

                repository.sendPacket(localAddress, packet.recipient(), ByteBuffer.wrap(bytes));
            } else {
                throw new RuntimeException("Bad message type");
            }
        }
    }

    void triggerRead(SocketAddress from, SocketAddress to, ByteBuffer data) {
        final AddressedEnvelope envelope = new DefaultAddressedEnvelope(Unpooled.wrappedBuffer(data), to, from);

        eventLoop().execute(new Runnable() {
            @Override
            public void run() {
                Collections.addAll(inboundBuffer, envelope);

                if (readInProgress) {
                    readInProgress = false;
                    for (;;) {
                        Object received = inboundBuffer.poll();
                        if (received == null) {
                            break;
                        }
                        pipeline().fireChannelRead(received);
                    }
                    pipeline().fireChannelReadComplete();
                }
            }
        });
    }

    @Override
    public ChannelConfig config() {
        return config;
    }

    @Override
    public boolean isOpen() {
        return state != State.CLOSED;
    }

    @Override
    public boolean isActive() {
        return state == State.BOUND;
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    private class LocalUnsafe extends AbstractUnsafe {

        @Override
        public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
            // this should never happen? no connection required
        }

    }
}
