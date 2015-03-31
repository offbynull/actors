package com.offbynull.peernetic.core.actors.unreliableproxy;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import static com.offbynull.peernetic.core.common.AddressUtils.SEPARATOR;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actors.unreliableproxy.Line.BufferMessage;
import com.offbynull.peernetic.core.common.AddressUtils;
import com.offbynull.peernetic.core.common.ByteBufferUtils;
import com.offbynull.peernetic.core.common.Serializer;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Collection;

public final class UnreliableProxyCoroutine implements Coroutine {

    @Override
    public void run(Continuation cont) throws Exception {
        Context ctx = (Context) cont.getContext();

        String selfAddr = ctx.getSelf();

        StartProxy startHub = ctx.getIncomingMessage();

        Serializer serializer = startHub.getSerializer();
        Line line = startHub.getLine();
        String timerPrefix = startHub.getTimerPrefix();

        while (true) {
            cont.suspend();
            Object msg = ctx.getIncomingMessage();
            Instant time = ctx.getTime();
            
            if (msg instanceof TransitMessage) {
                TransitMessage tMsg = (TransitMessage) msg;

                Collection<BufferMessage> msgs = line.messageArrive(time, tMsg);
                msgs.forEach(x -> {
                    String dstAddr = x.getDestination();
                    
                    String finalDstAddr = AddressUtils.relativize(selfAddr, dstAddr); // removes self prefix from dst addr

                    byte[] data = ByteBufferUtils.copyContentsToArray(x.getData());
                    Object dataObj = serializer.deserialize(data);

                    ctx.addOutgoingMessage(finalDstAddr, dataObj);
                });
            } else {
                String srcAddr = ctx.getSource();
                String dstAddr = ctx.getDestination();

                String finalSrcAddr = AddressUtils.parentize(selfAddr, srcAddr); // adds self prefix to src addr

                byte[] data = serializer.serialize(msg);
                ByteBuffer dataBuffer = ByteBuffer.wrap(data);
                BufferMessage bufferMessage = new BufferMessage(dataBuffer, finalSrcAddr, dstAddr);

                Collection<TransitMessage> msgs = line.messageDepart(time, bufferMessage);
                msgs.forEach(x -> {
                    ctx.addOutgoingMessage(timerPrefix + SEPARATOR + x.getDuration().toMillis() + SEPARATOR + x.getSource(), x);
                });
            }
        }
    }
}
