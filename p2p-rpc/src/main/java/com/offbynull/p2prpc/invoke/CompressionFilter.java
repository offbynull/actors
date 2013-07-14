package com.offbynull.p2prpc.invoke;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;

public class CompressionFilter implements Filter {

    @Override
    public byte[] modify(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOs = new GZIPOutputStream(baos)) {
            gzipOs.write(data);
        }
        return baos.toByteArray();
    }

    @Override
    public byte[] unmodify(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPInputStream gzipIs = new GZIPInputStream(
                    new ByteArrayInputStream(data));) {
            IOUtils.copy(gzipIs, baos);
        }
        return baos.toByteArray();
    }
}
