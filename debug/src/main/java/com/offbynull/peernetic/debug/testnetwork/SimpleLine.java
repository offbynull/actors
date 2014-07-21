package com.offbynull.peernetic.debug.testnetwork;

import com.offbynull.peernetic.debug.testnetwork.messages.ArriveMessage;
import com.offbynull.peernetic.debug.testnetwork.messages.DepartMessage;
import com.offbynull.peernetic.debug.testnetwork.messages.TransitMessage;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;

public final class SimpleLine<A> implements Line<A> {

    @Override
    public Collection<TransitMessage<A>> depart(DepartMessage<A> departMessage) {
        TransitMessage<A> transitMsg = new TransitMessage<>(departMessage.getSource(), departMessage.getDestination(),
                departMessage.getData(), Duration.ZERO);
        return Collections.singleton(transitMsg);
    }

    @Override
    public Collection<ArriveMessage<A>> arrive(TransitMessage<A> transitMessage) {
        ArriveMessage<A> arriveMessage = new ArriveMessage<>(transitMessage.getData(), transitMessage.getSource(),
                transitMessage.getDestination());
        return Collections.singleton(arriveMessage);
    }
}
