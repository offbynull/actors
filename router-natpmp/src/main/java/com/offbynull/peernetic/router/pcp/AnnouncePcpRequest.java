package com.offbynull.peernetic.router.pcp;

import java.nio.ByteBuffer;

public final class AnnouncePcpRequest extends PcpRequest {

    public AnnouncePcpRequest(PcpOption ... options) {
        super(0, 0L, options);
    }

    @Override
    protected void dumpOpCodeSpecificInformation(ByteBuffer dst) {
        // no opcode specific data
    }
    
}
