package com.offbynull.peernetic.core.gateways.udp;

import com.offbynull.peernetic.core.gateway.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.Validate;


final class SerializerEncodeHandler extends AbstractEncodeHandler {

    private Serializer serializer;

    SerializerEncodeHandler(Serializer serializer) {
        Validate.notNull(serializer);
        this.serializer = serializer;
    }

    @Override
    protected ByteBuf encode(Object obj) {
        Validate.notNull(obj);
        
        byte[] data = serializer.serialize(obj);
        return Unpooled.wrappedBuffer(data);
    }
}
