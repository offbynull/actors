package com.offbynull.peernetic.network.handlers;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.binary.BinaryStreamDriver;
import io.netty.buffer.ByteBuf;
import java.io.ByteArrayInputStream;
import org.apache.commons.lang3.Validate;

/**
 * A Netty handler that deserializes a {@link ByteBuf} to a {@link Object} using XStream.
 * @author Kasra Faghihi
 */
public final class XStreamDecodeHandler extends AbstractDecodeHandler {

    private XStream xstream;

    /**
     * Constructs a {@link XStreamDecodeHandler} with a binary XStream driver.
     */
    public XStreamDecodeHandler() {
        this(new XStream(new BinaryStreamDriver()));
    }

    /**
     * Constructs a {@link XStreamDecodeHandler}.
     * @param xstream XStream object
     * @throws NullPointerException if any argument is {@code null}
     */
    public XStreamDecodeHandler(XStream xstream) {
        Validate.notNull(xstream);
        this.xstream = xstream;
    }
    
    @Override
    protected Object decode(ByteBuf buf) {
        Validate.notNull(buf);
        
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        
        Object ret = xstream.fromXML(new ByteArrayInputStream(data));
        
        Validate.notNull(ret);
        
        return ret;
    }
    
}
