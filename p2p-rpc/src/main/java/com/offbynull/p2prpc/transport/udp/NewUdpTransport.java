package com.offbynull.p2prpc.transport.udp;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.offbynull.p2prpc.transport.IncomingMessage;
import com.offbynull.p2prpc.transport.IncomingMessageListener;
import com.offbynull.p2prpc.transport.IncomingResponse;
import com.offbynull.p2prpc.transport.OutgoingMessage;
import com.offbynull.p2prpc.transport.OutgoingMessageResponseListener;
import com.offbynull.p2prpc.transport.OutgoingResponse;
import com.offbynull.p2prpc.transport.Transport;
import com.offbynull.p2prpc.transport.udp.RequestManager.Result;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
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
        
        private MessageIdGenerator idGenerator;
        private RequestManager<InetSocketAddress> requestManager;
        

        public EventLoop() throws IOException {
            idGenerator = new MessageIdGenerator();
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
        protected void run() {
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
                    try {
                        channel.register(selector, selectionKey);
                    } catch (ClosedChannelException cce) {
                        throw new RuntimeException(cce);
                    }
                }
                
                // select
                Result timeoutRes = requestManager.evaluate(currentTime);
                long waitDuration = timeoutRes.getWaitDuration();
                try {
                    selector.select(waitDuration);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                // go through receivers requestManager has removed for timing out and add timeout events for each of them
                for (OutgoingMessageResponseListener<InetSocketAddress> receiver :  timeoutRes.getTimedOutReceivers()) {
                    internalEventQueue.add(new EventResponseTimedOut(receiver));
                }
                
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
                        try {
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
                                throw new IllegalStateException();
                            }
                        } catch (RuntimeException | IOException e) {
                            e.printStackTrace();
                        }
                    } else if (key.isWritable()) { // ready for outgoing data
                        try {
                            Command command = dumpedCommandQueue.poll();

                            if (command instanceof CommandSendRequest) {
                                CommandSendRequest commandSr = (CommandSendRequest) command;

                                OutgoingMessage<InetSocketAddress> request = commandSr.getMessage();
                                OutgoingMessageResponseListener<InetSocketAddress> receiver = commandSr.getResponseListener();

                                MessageId id = idGenerator.generate();

                                buffer.clear();

                                RequestResponseMarker.writeRequestMarker(buffer);
                                id.writeId(buffer);

                                buffer.put(request.getData());

                                InetSocketAddress dest = request.getTo();

                                requestManager.addRequestId(dest, id, receiver, currentTime);

                                channel.send(buffer, dest);
                            } else if (command instanceof CommandSendResponse) {
                                CommandSendResponse commandSr = (CommandSendResponse) command;

                                OutgoingResponse response = commandSr.getResponse();
                                InetSocketAddress dest = commandSr.getAddress();
                                MessageId id = commandSr.getMessageId();

                                buffer.clear();

                                RequestResponseMarker.writeResponseMarker(buffer);
                                id.writeId(buffer);

                                buffer.put(response.getData());

                                channel.send(buffer, dest);                        
                            } else {
                                throw new IllegalStateException();
                            }
                        } catch (RuntimeException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                
                processEvents(internalEventQueue);
                internalEventQueue.clear();
            }
        }

        private void processEvents(LinkedList<Event> internalEventQueue) {
            IncomingMessageListener[] handlersArray = incomingMessageListeners.toArray(new IncomingMessageListener[0]);
            
            for (Event event : internalEventQueue) {
                if (event instanceof EventRequestArrived) {
                    EventRequestArrived request = (EventRequestArrived) event;
                    
                    IncomingMessage<InetSocketAddress> data = request.getRequest();
                    Selector selector = request.getSelector();
                    MessageId id = request.getId();
                    UdpIncomingMessageResponseHandler responseSender = new UdpIncomingMessageResponseHandler(commandQueue, selector, id,
                            data.getFrom());

                    for (IncomingMessageListener handler : handlersArray) {
                        try {
                            handler.messageArrived(data, responseSender);
                        } catch (RuntimeException re) {
                            // don't bother notifying the others
                            break;
                        }
                    }
                } else if (event instanceof EventResponseArrived) {
                    EventResponseArrived response = (EventResponseArrived) event;
                    IncomingResponse<InetSocketAddress> data = response.getResponse();

                    try {
                        response.getReceiver().responseArrived(data);
                    } catch (RuntimeException re) {
                        // do nothing
                    }
                } else if (event instanceof EventResponseTimedOut) {
                    EventResponseArrived response = (EventResponseArrived) event;
                    IncomingResponse<InetSocketAddress> data = response.getResponse();

                    try {
                        response.getReceiver().timedOut();
                    } catch (RuntimeException re) {
                        // do nothing
                    }
                }
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
