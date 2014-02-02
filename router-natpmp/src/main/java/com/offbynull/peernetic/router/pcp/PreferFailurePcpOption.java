package com.offbynull.peernetic.router.pcp;

import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

public final class PreferFailurePcpOption extends PcpOption {
    
    public PreferFailurePcpOption(ByteBuffer buffer) {
        super(buffer);
        Validate.isTrue(super.getCode() == 2);
    }

    public PreferFailurePcpOption() {
        super(2, ByteBuffer.allocate(0));
    }
}
