package com.offbynull.p2prpc.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

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

        ByteBuffer writeBuf = ByteBuffer.wrap(data);

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
            channel.register(selector, SelectionKey.OP_READ
                    | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT);

            ByteBuffer readBuf = ByteBuffer.allocate(8192);
            ByteArrayOutputStream inputOs = new ByteArrayOutputStream();

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
                        if (channel.read(readBuf) == -1) {
                            return inputOs.toByteArray();
                        }

                        inputOs.write(readBuf.array(), 0, readBuf.position());
                        readBuf.clear();
                    } else if (key.isWritable()) {
                        channel.write(writeBuf);

                        if (writeBuf.remaining() == 0) {
                            channel.shutdownOutput();
                        }
                    } else if (key.isConnectable()) {
                        channel.finishConnect();
                    }
                }

                currentTime = System.currentTimeMillis();
            }

            if (currentTime >= endTime) {
                throw new IOException("Timed out");
            }

            throw new IOException("Unknown error");
        }
    }

    @Override
    public void stop() {
        if (state != State.STOPPED) {
            throw new IllegalStateException();
        }

        state = State.STOPPED;
    }

    private enum State {

        INIT,
        STARTED,
        STOPPED
    }

    public static void main(String[] args) throws Throwable {
        TcpClient tcpClient = new TcpClient();
        tcpClient.start();
        tcpClient.send(new InetSocketAddress("www.blizzard.com", 80),
                "GET /\r\n\r\n".getBytes());
        tcpClient.stop();
    }
}
