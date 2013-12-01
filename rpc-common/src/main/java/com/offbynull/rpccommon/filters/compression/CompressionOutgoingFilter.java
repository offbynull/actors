package com.offbynull.rpccommon.filters.compression;

import com.offbynull.rpc.transport.OutgoingFilter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.lang3.Validate;

/**
 * An {@link OutgoingFilter} that compresses data using {@link GZIPOutputStream}.
 * @author Kasra F
 * @param <A> address type
 */
public final class CompressionOutgoingFilter<A> implements OutgoingFilter<A> {

    @Override
    public ByteBuffer filter(A to, ByteBuffer buffer) {
        Validate.notNull(to);
        Validate.notNull(buffer);
        
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOs = new GZIPOutputStream(baos)) {
            gzipOs.write(data);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return ByteBuffer.wrap(baos.toByteArray());
    }
    
}
