package com.offbynull.peernetic.gateways.visualizer;

import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GraphRunnable implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphRunnable.class);

    private final Bus bus;

    public GraphRunnable(Bus bus) {
        this.bus = bus;
    }

    @Override
    public void run() {
        try {
            while (true) {
                // Poll for new messages
                List<Object> incomingObjects = bus.pull();
                Validate.notNull(incomingObjects);
                Validate.noNullElements(incomingObjects);

                GraphApplication graph = GraphApplication.getInstance();
                if (graph == null) {
                    // TODO log warning here
                    return;
                }

                List<Object> payloads = new ArrayList<>(incomingObjects.size());
                for (Object incomingObject : incomingObjects) {
                    Message msg = (Message) incomingObject;
                    
                    Object payload = msg.getMessage();
                    payloads.add(payload);
                }
                
                graph.execute(payloads);
            }
        } catch (InterruptedException ie) {
            bus.close(); // just in case
        }
    }

    private static final class PendingMessage implements Comparable<PendingMessage> {

        private final Instant sendTime;
        private final String from;
        private final String to;
        private final Object message;

        public PendingMessage(Instant sendTime, String from, String to, Object message) {
            Validate.notNull(from);
            Validate.notNull(to);
            Validate.notNull(message);
            this.sendTime = sendTime;
            this.from = from;
            this.to = to;
            this.message = message;
        }

        public Instant getSendTime() {
            return sendTime;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public Object getMessage() {
            return message;
        }

        @Override
        public int compareTo(PendingMessage o) {
            return sendTime.compareTo(o.sendTime);
        }

    }

}
