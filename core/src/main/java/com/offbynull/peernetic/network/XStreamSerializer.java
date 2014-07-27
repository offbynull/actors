package com.offbynull.peernetic.network;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.binary.BinaryStreamDriver;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.Validate;

public final class XStreamSerializer implements Serializer {

    private final XStream xstream;

    public XStreamSerializer() {
        this(new XStream(new BinaryStreamDriver()));
    }

    public XStreamSerializer(XStream xstream) {
        Validate.notNull(xstream);
        this.xstream = xstream;
    }

    @Override
    public byte[] serialize(Object obj) {
        Validate.notNull(obj);
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();) {
            xstream.toXML(obj, baos);
            return baos.toByteArray();
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    @Override
    public Object deserialize(byte[] data) {
        Validate.notNull(data);
        
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);) {
            return xstream.fromXML(bais);
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

}
