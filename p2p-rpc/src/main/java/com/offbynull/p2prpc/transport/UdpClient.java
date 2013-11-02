package com.offbynull.p2prpc.transport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import org.apache.commons.io.IOUtils;

public final class UdpClient implements Client {

    private State state = State.INIT;
    private long timeout;
    private int bufferSize;

    public UdpClient() {
        this(65535, 10000L);
    }

    public UdpClient(int bufferSize) {
        this(bufferSize, 10000L);
    }
    
    public UdpClient(int bufferSize, long timeout) {
        this.bufferSize = bufferSize;
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
    public byte[] send(InetSocketAddress to, byte[] data)
            throws IOException {

        if (state != State.STARTED) {
            throw new IllegalStateException();
        }

        // IO
        try (Selector selector = Selector.open();
                DatagramChannel channel = DatagramChannel.open();) {
            channel.configureBlocking(false);
            SelectionKey selectionKey = channel.register(selector,
                    SelectionKey.OP_WRITE);


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
                        ByteBuffer recvBuffer = ByteBuffer.wrap(new byte[bufferSize]);
                        channel.receive(recvBuffer);
                        
                        recvBuffer.flip();
                        byte[] inData = new byte[recvBuffer.remaining()];
                        recvBuffer.get(inData);
                        
                        return inData;
                    } else if (key.isWritable()) {
                        channel.send(ByteBuffer.wrap(data), to);
                        selectionKey.interestOps(SelectionKey.OP_READ);
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
