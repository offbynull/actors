package com.offbynull.peernetic.router.pcp;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

abstract class PcpRequest {
    private int op;
    private long lifetime;

    PcpRequest(int op, long lifetime) {
        Validate.inclusiveBetween(0, 127, op);
        Validate.inclusiveBetween(0L, 0xFFFFFFFFL, lifetime);

        this.op = op;
        this.lifetime = lifetime;
    }

    public int getOp() {
        return op;
    }

    public long getLifetime() {
        return lifetime;
    }

    public void dump(ByteBuffer dst, InetAddress selfAddress) {
        Validate.notNull(dst);
        Validate.notNull(selfAddress);
        
        dst.put((byte) 2);
        dst.put((byte) op); // topmost bit should be 0, because op is between 0 to 127, which means r-flag = 0
        dst.putShort((short) 0);
        dst.putInt((int) lifetime);
        
        byte[] selfAddressArr = selfAddress.getAddress();
        switch (selfAddressArr.length) {
            case 4: {
                // convert ipv4 address to ipv4-mapped ipv6 address
                for (int i = 0; i < 10; i++) {
                    dst.put((byte) 0);
                }
                
                for (int i = 0; i < 2; i++) {
                    dst.put((byte) 0xff);
                }
                
                dst.put(selfAddressArr);
                break;
            }
            case 16: {
                dst.put(selfAddressArr);
                break;
            }
            default:
                throw new IllegalArgumentException();
        }
        
        dumpOpCodeSpecificInformation(dst);
    }
    
    protected abstract void dumpOpCodeSpecificInformation(ByteBuffer dst);
}
