package com.offbynull.peernetic.nettyhelper.handlers.xstream;

import com.offbynull.peernetic.nettyhelper.handlers.common.AbstractEncodeHandler;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.binary.BinaryStreamDriver;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.ByteArrayOutputStream;
import org.apache.commons.lang3.Validate;

/**
 * A Netty handler that serializes an {@link Object} to a {@link ByteBuf} using XStream.
 * @author Kasra Faghihi
 */
public final class XStreamEncodeHandler extends AbstractEncodeHandler {

    private XStream xstream;

    /**
     * Constructs a {@link XStreamEncodeHandler} with a binary XStream driver.
     */
    public XStreamEncodeHandler() {
        this(new XStream(new BinaryStreamDriver()));
    }

    /**
     * Constructs a {@link XStreamEncodeHandler}.
     * @param xstream XStream object
     * @throws NullPointerException if any argument is {@code null}
     */
    public XStreamEncodeHandler(XStream xstream) {
        Validate.notNull(xstream);
        this.xstream = xstream;
    }

    @Override
    protected ByteBuf encode(Object obj) {
        Validate.notNull(obj);
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        xstream.toXML(obj, os);
        return Unpooled.wrappedBuffer(os.toByteArray());
    }
}
