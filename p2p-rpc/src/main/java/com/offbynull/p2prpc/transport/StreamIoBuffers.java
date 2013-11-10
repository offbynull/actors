package com.offbynull.p2prpc.transport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

public final class StreamIoBuffers {

    private State state = State.INIT;
    private Mode mode;
    private ByteArrayOutputStream readOs;
    private ByteArrayInputStream writeIs;

    public StreamIoBuffers(Mode mode) {
        Validate.notNull(mode);
        
        this.mode = mode;
    }
    
    public void startReading() {
        if ((mode == Mode.READ_FIRST && state != State.INIT)
                || (mode == Mode.WRITE_FIRST && state != State.WRITE_DONE)) {
            throw new IllegalStateException();
        }
        
        state = State.READ;
        readOs = new ByteArrayOutputStream();
    }

    public void addReadBlock(ByteBuffer buffer) {
        Validate.notNull(buffer);
        
        if (state != State.READ) {
            throw new IllegalStateException();
        }
        
        readOs.write(buffer.array(), 0, buffer.position());
    }

    public byte[] finishReading() {
        if (state != State.READ) {
            throw new IllegalStateException();
        }

        state = State.READ_DONE;
        byte[] data = readOs.toByteArray();
        readOs = null;
        return data;
    }

    public void startWriting(ByteBuffer data) {
        Validate.notNull(data);
        
        if ((mode == Mode.READ_FIRST && state != State.READ_DONE)
                || (mode == Mode.WRITE_FIRST && state != State.INIT)) {
            throw new IllegalStateException();
        }
        
        state = State.WRITE;
        byte[] dataCopy = new byte[data.limit()];
        data.mark();
        data.get(dataCopy);
        data.reset();

        writeIs = new ByteArrayInputStream(dataCopy);        
    }
    
    public void startWriting(byte[] data) {
        Validate.notNull(data);
        
        startWriting(ByteBuffer.wrap(data));
    }

    public void getWriteBlock(ByteBuffer buffer) {
        Validate.notNull(buffer);
        
        if (state != State.WRITE) {
            throw new IllegalStateException();
        }
        
        writeIs.mark(Integer.MAX_VALUE);
        
        int length = buffer.capacity();
        int offset = buffer.position();
        byte[] array = buffer.array();
        int amount = writeIs.read(array, offset, length);
        buffer.position(0);
        buffer.limit(amount == -1 ? 0 : amount);
    }
    
    public void adjustWritePointer(int amountWritten) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, amountWritten);
        
        if (state != State.WRITE) {
            throw new IllegalStateException();
        }
        
        writeIs.reset();
        writeIs.skip(amountWritten);
    }
    
    public void finishWriting() {
        if (state != State.WRITE) {
            throw new IllegalStateException();
        }
        
        state = State.WRITE_DONE;
        writeIs = null;
    }
    
    public boolean isDone() {
        switch (mode) {
            case READ_FIRST:
                return state == State.WRITE_DONE;
            case WRITE_FIRST:
                return state == State.READ_DONE;
            default:
                throw new IllegalStateException();
        }
    }

    public boolean isReading() {
        return state == State.READ;
    }
    
    public boolean isWriting() {
        return state == State.WRITE;
    }
    
    public boolean isEndOfWrite() {
        if (state != State.WRITE) {
            throw new IllegalStateException();
        }
        
        return writeIs.available() == 0;
    }

    public enum Mode {
        WRITE_FIRST,
        READ_FIRST
    }
    
    private enum State {
        INIT,
        READ,
        READ_DONE,
        WRITE,
        WRITE_DONE
    }
}
