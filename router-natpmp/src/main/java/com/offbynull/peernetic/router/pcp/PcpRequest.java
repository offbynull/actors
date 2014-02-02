package com.offbynull.peernetic.router.pcp;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.Validate;

public abstract class PcpRequest {
    private int op;
    private long lifetime;
    private List<PcpOption> options;

    PcpRequest(int op, long lifetime, PcpOption ... options) {
        Validate.inclusiveBetween(0, 127, op);
        Validate.inclusiveBetween(0L, 0xFFFFFFFFL, lifetime);
        Validate.noNullElements(options);

        this.op = op;
        this.lifetime = lifetime;
        this.options = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(options)));
    }

    public int getOp() {
        return op;
    }

    public long getLifetime() {
        return lifetime;
    }

    public List<PcpOption> getOptions() {
        return options;
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

        for (PcpOption option : options) {
            option.dump(dst);
        }
    }
    
    protected abstract void dumpOpCodeSpecificInformation(ByteBuffer dst);
}
