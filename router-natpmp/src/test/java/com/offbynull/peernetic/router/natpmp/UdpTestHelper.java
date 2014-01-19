package com.offbynull.peernetic.router.natpmp;

import com.offbynull.peernetic.common.utils.ByteBufferUtils;
import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class UdpTestHelper implements Closeable {
    public DatagramSocket socket;
    public Map<ByteBuffer, ByteBuffer> requestResponseMap;
    
    private UdpTestHelper(int port) throws IOException {
        socket = new DatagramSocket(port);
        requestResponseMap = Collections.synchronizedMap(new HashMap<ByteBuffer, ByteBuffer>());
    }
    
    public void addMapping(ByteBuffer request, ByteBuffer response) {
        requestResponseMap.put(
                ByteBufferUtils.copyContents(request).asReadOnlyBuffer(),
                ByteBufferUtils.copyContents(response).asReadOnlyBuffer());
    }
    
    public static UdpTestHelper create(int port) throws IOException {
        final UdpTestHelper helper = new UdpTestHelper(port);
        
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    while (true) {
                        byte[] buffer = new byte[65535];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        helper.socket.receive(packet);
                        
                        ByteBuffer request = ByteBuffer.wrap(buffer);
                        request.limit(packet.getLength());
                        
                        ByteBuffer response = helper.requestResponseMap.get(request);
                        
                        if (response != null) {
                            response = response.asReadOnlyBuffer();
                            int rem = response.remaining();
                            response.get(buffer, 0, rem);
                            packet.setLength(rem);
                            helper.socket.send(packet);
                        }
                    }
                } catch (IOException ioe) {
                    // do nothing
                }
            }
        });
        
        thread.start();
        return helper;
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
