package com.offbynull.peernetic.debug.testnetwork;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.commons.io.output.ByteArrayOutputStream;

public final class SimpleSerializer implements Serializer {

    private Kryo kryo = new Kryo();

    @Override
    public byte[] serialize(Object message) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream();
                Output output = new Output(os)) {
            kryo.writeClassAndObject(output, message);
            return os.toByteArray();
        } catch (IOException ioe) {
            // never happens
            throw new IllegalStateException(ioe);
        }
    }

    @Override
    public Object deserialize(byte[] buffer) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
                Input input = new Input(bais);) {
            return kryo.readClassAndObject(input);
        } catch (IOException ioe) {
            // never happens
            throw new IllegalStateException(ioe);
        }
    }

}
