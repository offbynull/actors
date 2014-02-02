package com.offbynull.peernetic.router.pcp;

import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

public final class AnnouncePcpResponse extends PcpResponse {

    public AnnouncePcpResponse(ByteBuffer buffer) {
        super(buffer);
        
        Validate.isTrue(super.getOp() == 0);
    }
}
