package com.offbynull.p2prpc.transport.udp;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.offbynull.p2prpc.transport.IncomingMessage;
import com.offbynull.p2prpc.transport.IncomingMessageListener;
import com.offbynull.p2prpc.transport.IncomingResponse;
import com.offbynull.p2prpc.transport.OutgoingMessage;
import com.offbynull.p2prpc.transport.OutgoingMessageResponseListener;
import com.offbynull.p2prpc.transport.Transport;
import com.offbynull.p2prpc.transport.udp.RequestManager.Result;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

public final class NewUdpTransport implements Transport {
    
    private InetSocketAddress listenAddress;
    private Selector selector;
    
    private int bufferSize;
    private long timeout;
    
    private LinkedBlockingQueue<Command> commandQueue;
    private LinkedBlockingQueue<IncomingMessageListener> incomingMessageListeners;
    
    private EventLoop eventLoop;
    private Lock accessLock;

    public NewUdpTransport(int port, int bufferSize, long timeout) throws IOException {
        this(new InetSocketAddress(port), bufferSize, timeout);
    }

    public NewUdpTransport(InetSocketAddress listenAddress, int bufferSize, long timeout) throws IOException {
        Validate.notNull(listenAddress);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, bufferSize);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, timeout);

        this.listenAddress = listenAddress;
        this.selector = Selector.open();
        
        this.bufferSize = bufferSize;
        this.timeout = timeout;
        
        this.commandQueue = new LinkedBlockingQueue<>();
        this.incomingMessageListeners = new LinkedBlockingQueue<>();
        
        accessLock = new ReentrantLock();
    }
    
    @Override
    public void start() throws IOException {
        accessLock.lock();
        try {
            if (eventLoop != null) {
                throw new IllegalStateException();
            }

            eventLoop = new EventLoop();
            eventLoop.startAndWait();
        } finally {
            accessLock.unlock();
        }
    }

    @Override
    public void stop() throws IOException {
        accessLock.lock();
        try {
            if (eventLoop == null || !eventLoop.isRunning()) {
                throw new IllegalStateException();
            }

            eventLoop.stopAndWait();
        } finally {
            accessLock.unlock();
        }
    }

    @Override
    public void sendMessage(OutgoingMessage message, OutgoingMessageResponseListener listener) {
        Validate.notNull(message);
        Validate.notNull(listener);
        Validate.validState(eventLoop != null && eventLoop.isRunning());
        
        commandQueue.add(new CommandSendRequest(message, listener));
        selector.wakeup();
    }

    @Override
    public void addMessageListener(IncomingMessageListener listener) {
        Validate.notNull(listener);
        Validate.validState(eventLoop != null && eventLoop.isRunning());
        
        incomingMessageListeners.add(listener);
    }

    @Override
    public void removeMessageListener(IncomingMessageListener listener) {
        Validate.notNull(listener);
        Validate.validState(eventLoop != null && eventLoop.isRunning());
        
        incomingMessageListeners.remove(listener);
    }
    
    private final class EventLoop extends AbstractExecutionThreadService {

        private DatagramChannel channel;
        private AtomicBoolean stop;
        
        private RequestManager<InetSocketAddress> requestManager;
        

        public EventLoop() throws IOException {
            requestManager = new RequestManager<>(timeout);
            
            try {
                channel = DatagramChannel.open();
            } catch (RuntimeException | IOException e) {
                IOUtils.closeQuietly(selector);
                IOUtils.closeQuietly(channel);
                throw e;
            }
        }
        
        @Override
        protected void startUp() throws IOException {
            stop = new AtomicBoolean(false);
            try {
                channel.configureBlocking(false);
                channel.register(selector, SelectionKey.OP_READ);
                channel.socket().bind(listenAddress);
            } catch (RuntimeException | IOException e) {
                IOUtils.closeQuietly(selector);
                IOUtils.closeQuietly(channel);
                throw e;
            }
        }

        @Override
        protected void run() throws Exception {
            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

            int selectionKey = SelectionKey.OP_READ;
            LinkedList<Event> internalEventQueue = new LinkedList<>();
            LinkedList<Command> dumpedCommandQueue = new LinkedList<>();
            while (true) {
                // get current time
                long currentTime = System.currentTimeMillis();
                
                // get outgoing data
                commandQueue.drainTo(dumpedCommandQueue);
                
                // set selection key based on if there's commands available -- this works because the only commands available are send req
                // and send resp
                int newSelectionKey = SelectionKey.OP_READ;
                if (!dumpedCommandQueue.isEmpty()) {
                    newSelectionKey |= SelectionKey.OP_WRITE;
                }
                
                if (newSelectionKey != selectionKey) {
                    selectionKey = newSelectionKey;
                    channel.register(selector, selectionKey);
                }
                
                // select
                Result timeoutRes = requestManager.evaluate(currentTime);
                long waitDuration = timeoutRes.getWaitDuration();
                selector.select(waitDuration);

                GO THROUGH TIMEOUTRES AND TRIGGER EVENTS SO METHODS WAITING FOR A RESPONSE DONT WAIT FOREVER;
                
                // stop if signalled
                if (stop.get()) {
                    return;
                }
                
                // update current time
                currentTime = System.currentTimeMillis();
                
                // go through selected keys
                Iterator keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = (SelectionKey) keys.next();
                    keys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isReadable()) { // incoming data available
                        InetSocketAddress from = (InetSocketAddress) channel.receive(buffer);

                        buffer.flip();
                        
                        if (RequestResponseMarker.isRequest(buffer)) {
                            RequestResponseMarker.skipOver(buffer);
                            
                            MessageId id = MessageId.extractPrependedId(buffer);
                            MessageId.skipOver(buffer);
                            
                            IncomingMessage<InetSocketAddress> request = new IncomingMessage<>(from, buffer, currentTime);
                            
                            EventRequestArrived eventRa = new EventRequestArrived(request, selector, id);
                            internalEventQueue.add(eventRa);
                        } else if (RequestResponseMarker.isResponse(buffer)) {
                            RequestResponseMarker.skipOver(buffer);
                            
                            MessageId id = MessageId.extractPrependedId(buffer);
                            MessageId.skipOver(buffer);
                            
                            OutgoingMessageResponseListener<InetSocketAddress> receiver = requestManager.getReceiver(from, id);
                            
                            if (receiver != null) { // timeout may have lapsed already, don't do anything if it did
                                IncomingResponse<InetSocketAddress> response = new IncomingResponse<>(from, buffer, currentTime);

                                EventResponseArrived eventRa = new EventResponseArrived(response, receiver);
                                internalEventQueue.add(eventRa);
                            }
                        } else {
                            // garbage, ignore it
                        }
                    } else if (key.isWritable()) { // ready for outgoing data
                        OutgoingMessage<InetSocketAddress> packet = commandQ.poll();
                        
                        if (packet != null) {
                            channel.send(packet.getData(), packet.getTo());
                        }
                    }
                }
                
                receiveNotifier.notify(pendingIncomingPackets);
                pendingIncomingPackets.clear();
            }
        }

        @Override
        protected void shutDown() throws Exception {
            IOUtils.closeQuietly(selector);
            IOUtils.closeQuietly(channel);
        }

        @Override
        protected String serviceName() {
            return UdpTransport.class.getSimpleName() + " Event Loop (" + listenAddress + ")";
        }

        @Override
        protected void triggerShutdown() {
            stop.set(true);
            selector.wakeup();
        }
    }
}
