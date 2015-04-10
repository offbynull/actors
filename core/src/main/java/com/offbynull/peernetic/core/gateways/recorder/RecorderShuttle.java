package com.offbynull.peernetic.core.gateways.recorder;

import com.offbynull.peernetic.core.common.AddressUtils;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.shuttle.Message;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;

final class RecorderShuttle implements Shuttle {
    
    private final String prefix;
    private final WriteBus internalBus;
    private final Shuttle dstShuttle;
    private final String dstAddress;

    RecorderShuttle(String prefix, WriteBus internalBus, Shuttle dstShuttle, String dstAddress) {
        Validate.notNull(prefix);
        Validate.notNull(internalBus);
        Validate.notNull(dstShuttle);
        Validate.notNull(dstAddress);
        Validate.isTrue(AddressUtils.isParent(dstShuttle.getPrefix(), dstAddress));

        this.prefix = prefix;
        this.internalBus = internalBus;
        this.dstShuttle = dstShuttle;
        this.dstAddress = dstAddress;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void send(Collection<Message> messages) {
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        
        internalBus.add(new MessageBlock(messages, Instant.now()));
        
        List<Message> redirectedMessages
                = messages.stream()
                .map(x -> {
                    String dstSuffix = AddressUtils.relativize(prefix, x.getDestinationAddress());
                    String realDstAddress = AddressUtils.parentize(dstAddress, dstSuffix);
                    return new Message(
                            x.getSourceAddress(),
                            realDstAddress,
                            x.getMessage());
                })
                .collect(Collectors.toList());
        dstShuttle.send(redirectedMessages);
    }

    
}
