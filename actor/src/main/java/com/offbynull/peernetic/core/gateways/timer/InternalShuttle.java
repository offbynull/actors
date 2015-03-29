package com.offbynull.peernetic.core.gateways.timer;

import com.offbynull.peernetic.core.actor.*;
import com.offbynull.peernetic.core.Shuttle;
import com.offbynull.peernetic.core.Message;
import static com.offbynull.peernetic.core.actor.ActorUtils.SEPARATOR;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InternalShuttle implements Shuttle {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalShuttle.class);

    private final String prefix;
    private final ScheduledExecutorService service;
    private final ConcurrentHashMap<String, Shuttle> outgoingShuttles;

    InternalShuttle(String prefix, ScheduledExecutorService service, ConcurrentHashMap<String, Shuttle> outgoingShuttles) {
        Validate.notNull(prefix);
        Validate.notNull(service);
        Validate.notNull(outgoingShuttles);
        Validate.notEmpty(prefix);

        this.prefix = prefix;
        this.service = service;
        this.outgoingShuttles = outgoingShuttles;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void send(Collection<Message> messages) {
        Validate.notNull(messages);
        Validate.noNullElements(messages);

        messages.forEach(x -> {
            try {
                String dst = x.getDestinationAddress();
                String dstPrefix = ActorUtils.getPrefix(dst);
                String dstId = ActorUtils.getId(dst);
                Validate.isTrue(dstPrefix.equals(prefix));
                Validate.isTrue(ActorUtils.getIdElementSize(dst) > 1);

                String[] splitDstId = dstId.split(SEPARATOR, 2);
                long delay = Long.parseLong(splitDstId[0]);
                Validate.isTrue(delay >= 0L);
                String sendAddr = splitDstId[1];
                Validate.notEmpty(sendAddr);

                service.schedule(() -> {
                    String sendPrefix = ActorUtils.getPrefix(sendAddr);
                    Shuttle shuttle = outgoingShuttles.get(sendPrefix);
                    
                    Message message = new Message(dst, sendAddr, x);
                    shuttle.send(Collections.singletonList(message));
                }, delay, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                LOGGER.error("Error shuttling message: " + x, e);
            }
        });
    }

}
