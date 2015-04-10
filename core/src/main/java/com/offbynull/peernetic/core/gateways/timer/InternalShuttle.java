package com.offbynull.peernetic.core.gateways.timer;

import com.offbynull.peernetic.core.common.AddressUtils;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.shuttle.Message;
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
                String src = x.getSourceAddress();
                String dst = x.getDestinationAddress();
                String dstPrefix = AddressUtils.getAddressElement(dst, 0);
                Validate.isTrue(dstPrefix.equals(prefix));

                String delayStr = AddressUtils.getAddressElement(dst, 1);
                long delay = Long.parseLong(delayStr);
                Validate.isTrue(delay >= 0L);

                service.schedule(() -> {
                    String sendPrefix = AddressUtils.getAddressElement(src, 0);
                    Shuttle shuttle = outgoingShuttles.get(sendPrefix);
                    
                    Message message = new Message(dst, src, x.getMessage());
                    shuttle.send(Collections.singletonList(message));
                }, delay, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                LOGGER.error("Error shuttling message: " + x, e);
            }
        });
    }

}
