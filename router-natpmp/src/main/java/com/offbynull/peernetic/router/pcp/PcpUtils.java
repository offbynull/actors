package com.offbynull.peernetic.router.pcp;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class PcpUtils {
    private PcpUtils() {
        // do nothing
    }
    
    static final InetAddress convertToIpv6(InetAddress address) {
        byte[] addrArr = address.getAddress();
        switch (addrArr.length) {
            case 4: {
                // convert ipv4 address to ipv4-mapped ipv6 address
                byte[] newAddrArr = new byte[16];
                newAddrArr[10] = (byte) 0xff;
                newAddrArr[11] = (byte) 0xff;
                System.arraycopy(addrArr, 0, newAddrArr, 12, 4);
                
                try {
                    return InetAddress.getByAddress(newAddrArr);
                } catch (UnknownHostException uhe) {
                    throw new IllegalStateException(uhe);
                }
            }
            case 16: {
                return address;
            }
            default:
                throw new IllegalStateException();
        }
    }

    static final InetAddress convertToIpv4IfPossible(InetAddress address) {
        byte[] addrArr = address.getAddress();
        switch (addrArr.length) {
            case 4: {
                return address;
            }
            case 16: {
                for (int i = 0; i < 10; i++) {
                    if (addrArr[i] != 0) {
                        return address;
                    }
                }

                for (int i = 10; i < 12; i++) {
                    if ((byte) addrArr[i] != (byte) 0xFF) {
                        return address;
                    }
                }
                
                byte[] v4AddrPartArr = new byte[4];
                System.arraycopy(addrArr, 12, v4AddrPartArr, 0, 4);
                try {
                    return InetAddress.getByAddress(v4AddrPartArr);
                } catch (UnknownHostException uhe) {
                    throw new IllegalStateException(uhe);
                }
            }
            default:
                throw new IllegalStateException();
        }
    }
}
