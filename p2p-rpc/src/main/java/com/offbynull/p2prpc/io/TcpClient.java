package com.offbynull.p2prpc.io;

import com.offbynull.p2prpc.io.StreamedIoBuffers.Mode;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import org.apache.commons.io.IOUtils;

public final class TcpClient implements Client {

    private State state = State.INIT;
    private long timeout;

    public TcpClient() {
        this(10000L);
    }

    public TcpClient(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public void start() {
        if (state != State.INIT) {
            throw new IllegalStateException();
        }

        state = State.STARTED;
    }

    @Override
    public byte[] send(InetSocketAddress address, byte[] data)
            throws IOException {

        if (state != State.STARTED) {
            throw new IllegalStateException();
        }

        ByteBuffer buffer = ByteBuffer.allocate(8192);
        StreamedIoBuffers buffers = new StreamedIoBuffers(Mode.WRITE_FIRST);
        buffers.startWriting(data);

        // IO
        try (Selector selector = Selector.open();
                SocketChannel channel = SocketChannel.open();) {
            channel.configureBlocking(false);
            channel.socket().setKeepAlive(true);
            channel.socket().setReuseAddress(true);
            channel.socket().setSoLinger(false, 0);
            channel.socket().setSoTimeout(0);
            channel.socket().setTcpNoDelay(true);
            channel.connect(address);
            SelectionKey selectionKey = channel.register(selector,
                    SelectionKey.OP_CONNECT);


            long currentTime = System.currentTimeMillis();
            long endTime = currentTime + timeout;
            while (currentTime < endTime) {
                selector.select(endTime - currentTime);

                Iterator keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = (SelectionKey) keys.next();
                    keys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isReadable()) {
                        buffer.clear();
                        if (channel.read(buffer) == -1) {
                            IOUtils.closeQuietly(channel);
                            return buffers.finishReading();
                        } else {
                            buffers.addReadBlock(buffer);
                        }
                    } else if (key.isWritable()) {
                        buffer.clear();
                        buffers.getWriteBlock(buffer);

                        if (buffer.limit()== 0) {
                            selectionKey.interestOps(SelectionKey.OP_READ);
                            channel.shutdownOutput();
                            buffers.finishWriting();
                            buffers.startReading();
                        } else {
                            int amountWritten = channel.write(buffer);
                            buffers.adjustWritePointer(amountWritten);
                        }
                    } else if (key.isConnectable()) {
                        channel.finishConnect();
                        selectionKey.interestOps(SelectionKey.OP_WRITE);
                    }
                }

                currentTime = System.currentTimeMillis();
            }

            IOUtils.closeQuietly(channel);
            
            if (currentTime >= endTime) {
                throw new IOException("Timed out");
            }

            throw new IOException("Unknown error");
        }
    }

    @Override
    public void stop() {
        if (state != State.STARTED) {
            throw new IllegalStateException();
        }

        state = State.STOPPED;
    }

    private enum State {

        INIT,
        STARTED,
        STOPPED
    }
}
