package com.offbynull.p2prpc.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public final class StreamedIoBuffers {

    private State state = State.INIT;
    private ByteArrayOutputStream readOs;
    private ByteArrayInputStream writeIs;

    public void startReading() {
        if (state != State.INIT) {
            throw new IllegalStateException();
        }
        
        state = State.READ;
        readOs = new ByteArrayOutputStream();
    }

    public void addReadBlock(ByteBuffer buffer) {
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

    public void startWriting(byte[] data) {
        if (state != State.READ_DONE) {
            throw new IllegalStateException();
        }
        
        state = State.WRITE;
        writeIs = new ByteArrayInputStream(data);
    }

    public void getWriteBlock(ByteBuffer buffer) {
        if (state != State.WRITE) {
            throw new IllegalStateException();
        }
        
        writeIs.mark(Integer.MAX_VALUE);
        
        int length = buffer.remaining();
        int offset = buffer.position();
        buffer.put(buffer.array(), offset, length);
    }
    
    public void adjustWritePointer(int amountWritten) {
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
        
        state = State.DONE;
        writeIs = null;
    }
    
    public boolean isDone() {
        return state == State.DONE;
    }
    
    private enum State {
        INIT,
        READ,
        READ_DONE,
        WRITE,
        DONE
    }
}
