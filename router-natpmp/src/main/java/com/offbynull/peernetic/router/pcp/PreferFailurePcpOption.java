package com.offbynull.peernetic.router.pcp;

import java.nio.ByteBuffer;

public final class PreferFailurePcpOption extends PcpOption {
    
    public PreferFailurePcpOption(ByteBuffer buffer) {
        super(buffer);
    }

    public PreferFailurePcpOption() {
        super(2, ByteBuffer.allocate(0));
    }
}
