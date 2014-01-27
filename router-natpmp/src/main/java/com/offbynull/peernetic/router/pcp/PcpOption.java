package com.offbynull.peernetic.router.pcp;

import com.offbynull.peernetic.common.utils.ByteBufferUtils;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

abstract class PcpOption {
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
        Validate.notNull(data);
        
        this.code = code;
        this.length = data.remaining();
        this.data = ByteBuffer.wrap(new byte[length + (length % 4)]);
        this.data = ByteBufferUtils.copyContents(data).asReadOnlyBuffer();
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
    
    
}
