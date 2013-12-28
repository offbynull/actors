/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.rpc.transports.tcp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

final class StreamIoBuffers {

    private State state = State.INIT;
    private Mode mode;
    private int readLimit;
    private int writeLimit;
    private CustomByteArrayOutputStream readOs;
    private CustomByteArrayInputStream writeIs;

    StreamIoBuffers(Mode mode, int readLimit, int writeLimit) {
        Validate.notNull(mode);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, readLimit);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, writeLimit);
        
        this.mode = mode;
        this.readLimit = readLimit;
        this.writeLimit = writeLimit;
    }
    
    public void startReading() {
        if ((mode == Mode.READ_FIRST && state != State.INIT) // NOPMD
                || (mode == Mode.WRITE_FIRST && state != State.WRITE_DONE)) { // NOPMD
            throw new IllegalStateException();
        }
        
        state = State.READ;
        readOs = new CustomByteArrayOutputStream();
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
        
        ByteBuffer dataBuffer = ByteBuffer.wrap(data);
        int size = dataBuffer.getInt();
        
        byte[] pureData = new byte[size];
        dataBuffer.get(pureData);
        
        Validate.isTrue(!dataBuffer.hasRemaining());
        
        readOs = null;
        return pureData;
    }

    public void startWriting(ByteBuffer data) {
        Validate.notNull(data);
        
        if ((mode == Mode.READ_FIRST && state != State.READ_DONE) // NOPMD
                || (mode == Mode.WRITE_FIRST && state != State.INIT)) { // NOPMD
            throw new IllegalStateException();
        }
        
        state = State.WRITE;
        
        int size = data.remaining();
        
        ByteBuffer dataCopy = ByteBuffer.allocate(4 + size);
        dataCopy.putInt(size);
        byte[] dataCopyArr = dataCopy.array();
        
        data.mark();
        data.get(dataCopyArr, 4, size);
        data.reset();

        writeIs = new CustomByteArrayInputStream(dataCopyArr);        
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
    
    private final class CustomByteArrayOutputStream extends ByteArrayOutputStream {

        @Override
        public void write(byte[] b, int off, int len) {
            Validate.isTrue(this.count + len <= readLimit, "Read limit exceeded");

            super.write(b, off, len);
        }

        @Override
        public void write(int b) {
            Validate.isTrue(this.count + 1 <= readLimit, "Read limit exceeded");
            
            super.write(b);
        }
        
    }
    
    private final class CustomByteArrayInputStream extends ByteArrayInputStream {

        public CustomByteArrayInputStream(byte[] buf) {
            super(buf);
            
            Validate.isTrue(buf.length <= writeLimit, "Write limit exceeded");
        }

        public CustomByteArrayInputStream(byte[] buf, int offset, int length) {
            super(buf, offset, length);
            
            Validate.isTrue(length <= writeLimit, "Write limit exceeded");
        }
        
    }
}
