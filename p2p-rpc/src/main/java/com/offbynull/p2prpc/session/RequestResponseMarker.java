package com.offbynull.p2prpc.session;

import java.nio.ByteBuffer;

public final class RequestResponseMarker {
    public static byte[] prependRequestMarker(byte[] data) {
        byte[] ret = new byte[data.length + 1];
        ret[0] = 0;
        System.arraycopy(data, 0, ret, 1, data.length);
        return ret;
    }
    
    public static byte[] prependResponseMarker(byte[] data) {
        byte[] ret = new byte[data.length + 1];
        ret[0] = 1;
        System.arraycopy(data, 0, ret, 1, data.length);
        return ret;
    }
    
    public static boolean isRequest(byte[] data) {
        return isRequest(ByteBuffer.wrap(data));
    }
    
    public static boolean isResponse(byte[] data) {
        return isResponse(ByteBuffer.wrap(data));
    }
    
    public static boolean isRequest(ByteBuffer data) {
        if (data.limit() == 0) {
            return false;
        }
        
        return data.get(0) == 0;
    }
    
    public static boolean isResponse(ByteBuffer data) {
        if (data.limit() == 0) {
            return false;
        }
        
        return data.get(0) == 1;
    }
}
