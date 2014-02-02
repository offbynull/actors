package com.offbynull.peernetic.router.pcp;

import java.nio.ByteBuffer;

public final class AnnouncePcpRequest extends PcpRequest {

    public AnnouncePcpRequest() {
        super(0, 0L);
    }

    @Override
    protected void dumpOpCodeSpecificInformation(ByteBuffer dst) {
        // no opcode specific data
    }
    
}
