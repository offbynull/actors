package com.offbynull.peernetic.router.pcp;

import java.net.InetAddress;

public final class PcpUtils {
    private PcpUtils() {
        // do nothing
    }
    
    static final byte[] convertToIpv6Array(InetAddress address) {
        byte[] addrArr = address.getAddress();
        switch (addrArr.length) {
            case 4: {
                // convert ipv4 address to ipv4-mapped ipv6 address
                byte[] newAddrArr = new byte[16];
                newAddrArr[10] = (byte) 0xff;
                newAddrArr[11] = (byte) 0xff;
                System.arraycopy(addrArr, 0, newAddrArr, 12, 4);
                
                return newAddrArr;
            }
            case 16: {
                return addrArr;
            }
            default:
                throw new IllegalStateException();
        }
    }
}
