/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.core.test;

import com.offbynull.peernetic.core.common.Serializer;
import com.offbynull.peernetic.core.gateways.recorder.RecordedBlock;
import com.offbynull.peernetic.core.gateways.recorder.RecordedMessage;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

public final class RecordMessageSink implements MessageSink {
    private final String destinationPrefix;
    private final DataOutputStream dos;
    private final Serializer serializer;

    public RecordMessageSink(String destinationPrefix, File file, Serializer serializer) throws IOException {
        Validate.notNull(destinationPrefix);
        Validate.notNull(file);
        Validate.notNull(serializer);
        this.destinationPrefix = destinationPrefix;
        this.dos = new DataOutputStream(new FileOutputStream(file));
        this.serializer = serializer;
    }
    
    @Override
    public void writeNextMessage(String source, String destination, Instant time, Object message) throws IOException {
        if (!AddressUtils.isPrefix(destinationPrefix, destination)) {
            return;
        }
        
        RecordedMessage recordedMessage = new RecordedMessage(
                source,
                AddressUtils.relativize(destinationPrefix, destination),
                message);
        RecordedBlock recordedBlock = new RecordedBlock(Collections.singletonList(recordedMessage), time);

        byte[] data = serializer.serialize(recordedBlock);
        dos.writeBoolean(true);
        dos.writeInt(data.length);
        IOUtils.write(data, dos);
    }

    @Override
    public void close() throws Exception {
        dos.writeBoolean(false);
        IOUtils.closeQuietly(dos);
    }
    
}
