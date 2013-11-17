package com.offbynull.p2prpc.transport.udp;

import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

public final class RequestResponseMarker {
    public static void writeRequestMarker(ByteBuffer data) {
        Validate.notNull(data);
        Validate.isTrue(data.remaining() >= 1);
        
        data.put((byte) 0);
    }
    
    public static void writeResponseMarker(ByteBuffer data) {
        Validate.notNull(data);
        Validate.isTrue(data.remaining() >= 1);
        
        data.put((byte) 1);
    }
    
    public static byte[] prependRequestMarker(byte[] data) {
        Validate.notNull(data);
        
        byte[] ret = new byte[data.length + 1];
        ret[0] = 0;
        System.arraycopy(data, 0, ret, 1, data.length);
        return ret;
    }
    
    public static byte[] prependResponseMarker(byte[] data) {
        Validate.notNull(data);
        
        byte[] ret = new byte[data.length + 1];
        ret[0] = 1;
        System.arraycopy(data, 0, ret, 1, data.length);
        return ret;
    }
    
    public static boolean isRequest(byte[] data) {
        Validate.notNull(data);
        
        return isRequest(ByteBuffer.wrap(data));
    }
    
    public static boolean isResponse(byte[] data) {
        Validate.notNull(data);
        
        return isResponse(ByteBuffer.wrap(data));
    }
    
    public static boolean isRequest(ByteBuffer data) {
        Validate.notNull(data);
        
        if (data.remaining()== 0) {
            return false;
        }
        
        return data.get(data.position()) == 0;
    }
    
    public static boolean isResponse(ByteBuffer data) {
        Validate.notNull(data);
        
        if (data.remaining() == 0) {
            return false;
        }
        
        return data.get(data.position()) == 1;
    }
    
    public static void skipOver(ByteBuffer data) {
        Validate.notNull(data);
        Validate.isTrue(data.remaining() > 0);
        
        data.position(data.position() + 1);
    }
}
