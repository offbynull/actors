package com.offbynull.peernetic.eventframework.impl.network.simpletcp;

import com.offbynull.peernetic.eventframework.impl.network.message.Response;
import com.offbynull.peernetic.eventframework.event.DefaultErrorIncomingEvent;
import com.offbynull.peernetic.eventframework.event.DefaultSuccessIncomingEvent;
import com.offbynull.peernetic.eventframework.event.OutgoingEvent;
import com.offbynull.peernetic.eventframework.handler.EventQueuePair;
import com.offbynull.peernetic.eventframework.handler.Handler;
import com.offbynull.peernetic.eventframework.handler.IncomingEventQueue;
import com.offbynull.peernetic.eventframework.handler.OutgoingEventQueue;
import com.offbynull.peernetic.eventframework.helper.SimpleIterateService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class CommunicationHandler implements Handler {

    private static final Set<Class<? extends OutgoingEvent>> HANDLED_EVENTS;

    static {
        Set<Class<? extends OutgoingEvent>> set = new HashSet<>();
        set.add(SendResponseOutgoingEvent.class);
        set.add(StartServerOutgoingEvent.class);
        set.add(StopServerOutgoingEvent.class);
        set.add(SendMessageOutgoingEvent.class);
        HANDLED_EVENTS = Collections.unmodifiableSet(set);
    }
    private Service service;

    public CommunicationHandler() {
        service = new Service();
    }

    @Override
    public Set<Class<? extends OutgoingEvent>> viewHandledEvents() {
        return HANDLED_EVENTS;
    }

    @Override
    public OutgoingEventQueue start(IncomingEventQueue incomingEventQueue) {
        if (incomingEventQueue == null) {
            throw new NullPointerException();
        }

        OutgoingEventQueue outgoingEventQueue =
                new OutgoingEventQueue(HANDLED_EVENTS);

        EventQueuePair eventQueuePair =
                new EventQueuePair(incomingEventQueue, outgoingEventQueue);
        service.safeStartAndWait(eventQueuePair);

        return outgoingEventQueue;
    }

    @Override
    public void stop() {
        service.safeStopAndWait();
    }

    private static final class Service extends SimpleIterateService {

        private IncomingEventQueue incomingEventQueue;
        private OutgoingEventQueue outgoingEventQueue;
        private Map<Long, MessageServer> serverByTrackedId;
        private MessageClient client;
        private ThreadPoolExecutor threadPool;

        @Override
        public void startUp() throws InterruptedException {
            EventQueuePair eventQueuePair =
                    (EventQueuePair) getPassedInObject(0);
            incomingEventQueue = eventQueuePair.getIncomingEventQueue();
            outgoingEventQueue = eventQueuePair.getOutgoingEventQueue();
            threadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60,
                    TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
            serverByTrackedId = new HashMap<>();
            client = new MessageClient(incomingEventQueue, threadPool);
            client.start();
        }

        @Override
        public boolean iterate() throws InterruptedException {
            List<OutgoingEvent> events = new ArrayList<>();
            outgoingEventQueue.waitForEvents(events);

            for (OutgoingEvent event : events) {
                Class<? extends OutgoingEvent> cls = event.getClass();
                if (cls == SendMessageOutgoingEvent.class) {
                    sendMessage((SendMessageOutgoingEvent) event);
                } else if (cls == StartServerOutgoingEvent.class) {
                    startServer((StartServerOutgoingEvent) event);
                } else if (cls == StopServerOutgoingEvent.class) {
                    stopServer((StopServerOutgoingEvent) event);
                } else if (cls == SendResponseOutgoingEvent.class) {
                    sendResponse((SendResponseOutgoingEvent) event);
                } else {
                    // Something went wrong here
                }
            }

            return true;
        }

        @Override
        public void shutDown() throws InterruptedException {
            for (MessageServer server : serverByTrackedId.values()) {
                try {
                    server.stop();
                } catch (RuntimeException re) {
                    // do nothing
                }
            }
            
            threadPool.shutdownNow();
        }

        private void sendMessage(SendMessageOutgoingEvent smoe) {
            long trackedId = smoe.getTrackedId();

            try {
                client.sendMessage(smoe.getHost(), smoe.getPort(),
                        smoe.getRequest(), trackedId);
            } catch (RuntimeException e) {
                DefaultErrorIncomingEvent errorInEvent =
                        new DefaultErrorIncomingEvent(trackedId, e);
                incomingEventQueue.push(errorInEvent);
            }
        }

        private void sendResponse(SendResponseOutgoingEvent sroe) {
            long trackedId = sroe.getTrackedId();
            long pendingId = sroe.getPendingId();
            Response response = sroe.getResponse();

            if (!serverByTrackedId.containsKey(trackedId)) {
                // don't send error
                return;
            }

            try {
                MessageServer tcpServer = serverByTrackedId.remove(trackedId);
                tcpServer.incomingResponse(response, pendingId);
            } catch (RuntimeException e) {
                // don't send error
            }
        }
        
        private void startServer(StartServerOutgoingEvent ssoe) {
            long trackedId = ssoe.getTrackedId();
            int port = ssoe.getPort();

            MessageServer tcpServer = null;
            try {
                tcpServer = new MessageServer(port, incomingEventQueue,
                        trackedId, threadPool);
                tcpServer.start();
                serverByTrackedId.put(trackedId, tcpServer);
            } catch (IOException | RuntimeException e) {
                DefaultErrorIncomingEvent errorInEvent =
                        new DefaultErrorIncomingEvent(trackedId, e);
                incomingEventQueue.push(errorInEvent);

                try {
                    tcpServer.stop();
                } catch (Exception unused) {
                    // do nothing
                }
            }
            
            DefaultSuccessIncomingEvent successInEvent =
                    new DefaultSuccessIncomingEvent(trackedId);
            incomingEventQueue.push(successInEvent);
        }
        
        private void stopServer(StopServerOutgoingEvent ssoe) {
            long trackedId = ssoe.getTrackedId();

            if (serverByTrackedId.containsKey(trackedId)) {
                DefaultErrorIncomingEvent errorInEvent =
                        new DefaultErrorIncomingEvent(trackedId,
                        "Server does not exist for this tracking id", null);
                incomingEventQueue.push(errorInEvent);
                return;
            }

            try {
                MessageServer tcpServer = serverByTrackedId.remove(trackedId);
                tcpServer.stop();
            } catch (RuntimeException e) {
                DefaultErrorIncomingEvent errorInEvent =
                        new DefaultErrorIncomingEvent(trackedId, e);
                incomingEventQueue.push(errorInEvent);
            }

            DefaultSuccessIncomingEvent successInEvent =
                    new DefaultSuccessIncomingEvent(trackedId);
            incomingEventQueue.push(successInEvent);
        }
    }
}
