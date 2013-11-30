package com.offbynull.rpccommon.filters.compression;

import com.offbynull.rpc.transport.IncomingFilter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

/**
 * An {@link IncomingFilter} that compresses data using {@link GZIPIncomingStream}.
 * @author Kasra F
 * @param <A> address type
 */
public class DecompressionFilter<A> implements IncomingFilter<A> {

    @Override
    public ByteBuffer filter(A from, ByteBuffer buffer) {
        Validate.notNull(from);
        Validate.notNull(buffer);
        
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPInputStream gzipIs = new GZIPInputStream(
                    new ByteArrayInputStream(data));) {
            IOUtils.copy(gzipIs, baos);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return ByteBuffer.wrap(baos.toByteArray());
    }
    
}
