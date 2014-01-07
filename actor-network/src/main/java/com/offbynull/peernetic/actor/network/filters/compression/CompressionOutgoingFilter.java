/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.actor.network.filters.compression;

import com.offbynull.peernetic.actor.network.OutgoingFilter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.lang3.Validate;

/**
 * An {@link OutgoingFilter} that compresses data using {@link GZIPOutputStream}.
 * @author Kasra Faghihi
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
