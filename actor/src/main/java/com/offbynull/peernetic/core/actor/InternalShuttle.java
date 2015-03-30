package com.offbynull.peernetic.core.actor;

import com.offbynull.peernetic.core.common.AddressUtils;
import com.offbynull.peernetic.core.Shuttle;
import com.offbynull.peernetic.core.Message;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InternalShuttle implements Shuttle {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalShuttle.class);
    
    private String prefix;
    private InternalBus bus;

    InternalShuttle(String prefix, InternalBus bus) {
        Validate.notNull(prefix);
        Validate.notEmpty(prefix);
        Validate.notNull(bus);

        this.prefix = prefix;
        this.bus = bus;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void send(Collection<Message> messages) {
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        
        List<Message> filteredMessages = new ArrayList<>(messages.size());
        messages.stream().forEach(x -> {
            try {
                String dst = x.getDestinationAddress();
                String dstPrefix = AddressUtils.getPrefix(dst);
                Validate.isTrue(dstPrefix.equals(prefix));
                
                filteredMessages.add(x);
            } catch (Exception e) {
                LOGGER.error("Error shuttling message: " + x, e);
            }
        });
        
        bus.add(filteredMessages);
    }

}
