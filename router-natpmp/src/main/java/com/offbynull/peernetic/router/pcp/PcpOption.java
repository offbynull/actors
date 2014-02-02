package com.offbynull.peernetic.router.pcp;

import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

public abstract class PcpOption {
    private int code;
    private int length;
    private ByteBuffer data;

    public PcpOption(ByteBuffer buffer) {
        Validate.notNull(buffer);
        
        code = buffer.get() & 0xFF;
        
        buffer.get(); // skip over reserved
        
        length = buffer.getShort() & 0xFFFF;
        
        byte[] dataArr = new byte[length];
        buffer.get(dataArr);
        
        data = ByteBuffer.wrap(dataArr).asReadOnlyBuffer();
        
        // skip over padding
        int remainder = length % 4;
        for (int i = 0; i < remainder; i++) {
            buffer.get();
        }
    }

    public PcpOption(int code, ByteBuffer data) {
        Validate.inclusiveBetween(0, 255, code);
        Validate.inclusiveBetween(0, 65535, data.remaining());
        Validate.notNull(data);
        
        this.code = code;
        this.length = data.remaining();
        this.data = ByteBuffer.allocate(length + (length % 4)).put(data).asReadOnlyBuffer();
    }

    public int getCode() {
        return code;
    }

    public int getLength() {
        return length;
    }

    public ByteBuffer getData() {
        return data.asReadOnlyBuffer();
    }

    void dump(ByteBuffer dst) {
        Validate.notNull(data);
        
        dst.put((byte) code);
        dst.put((byte) 0); // reserved
        dst.putShort((short) length);
        dst.put(data.asReadOnlyBuffer());
    }
    
}
