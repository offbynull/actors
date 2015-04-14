package com.offbynull.peernetic.core.shuttles.simple;

import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleShuttle implements Shuttle {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleShuttle.class);
    
    private final String prefix;
    private final Bus bus;

    public SimpleShuttle(String prefix, Bus bus) {
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
                String dstPrefix = AddressUtils.getAddressElement(dst, 0);
                Validate.isTrue(dstPrefix.equals(prefix));
                
                filteredMessages.add(x);
            } catch (Exception e) {
                LOGGER.error("Error shuttling message: " + x, e);
            }
        });
        
        bus.add(filteredMessages);
    }
}
