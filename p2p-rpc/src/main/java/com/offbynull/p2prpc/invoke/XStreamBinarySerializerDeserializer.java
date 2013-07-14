package com.offbynull.p2prpc.invoke;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.binary.BinaryStreamDriver;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class XStreamBinarySerializerDeserializer implements Serializer,
        Deserializer {
    
    private XStream xstream;
    
    public XStreamBinarySerializerDeserializer() {
        xstream = new XStream(new BinaryStreamDriver());
    }

    @Override
    public byte[] serializeMethodCall(InvokeData invokeData) {
        return serialize(SerializationType.METHOD_CALL, invokeData);
    }

    @Override
    public byte[] serializeMethodReturn(Object ret) {
        return serialize(SerializationType.METHOD_RETURN, ret);
    }

    @Override
    public byte[] serializeMethodThrow(Throwable err) {
        return serialize(SerializationType.METHOD_THROW, err);
    }
    
    private byte[] serialize(SerializationType type, Object obj) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(type.ordinal());
        xstream.toXML(obj, os);
        return os.toByteArray();
    }
    
    @Override
    public DeserializerResult deserialize(byte[] data) {
        ByteArrayInputStream is = new ByteArrayInputStream(data);
        int ordinal = is.read();
        SerializationType type = SerializationType.values()[ordinal];
        Object obj = xstream.fromXML(is);
        
        switch (type) {
            case METHOD_CALL:
                if (!(obj instanceof InvokeData)) {
                    throw new IllegalArgumentException("Inconsistent type");
                }
                break;
            case METHOD_RETURN:
                break;
            case METHOD_THROW:
                if (!(obj instanceof Throwable)) {
                    throw new IllegalArgumentException("Inconsistent type");
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
        
        return new DeserializerResult(type, obj);
    }
}
