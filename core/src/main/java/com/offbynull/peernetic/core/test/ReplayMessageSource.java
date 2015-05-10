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
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

public final class ReplayMessageSource implements MessageSource {
    private final String destinationPrefix;
    private final DataInputStream dis;
    private final Serializer serializer;
    
    private Instant lastTime;
    private Duration duration;
    private Iterator<RecordedMessage> msgIt;

    public ReplayMessageSource(String destinationPrefix, File file, Serializer serializer) throws IOException {
        Validate.notNull(destinationPrefix);
        Validate.notNull(file);
        Validate.notNull(serializer);
        this.destinationPrefix = destinationPrefix;
        this.dis = new DataInputStream(new FileInputStream(file));
        this.serializer = serializer;
    }
    
    @Override
    public SourceMessage readNextMessage() throws IOException {
        if (msgIt == null || !msgIt.hasNext()) {
            boolean hasMore = dis.readBoolean();
            if (!hasMore) {
                return null; // signals that the source is done and should be removed
            }

            int size = dis.readInt();
            byte[] data = new byte[size];

            IOUtils.readFully(dis, data);
            RecordedBlock recordedBlock = (RecordedBlock) serializer.deserialize(data);

            Instant newLastTime = recordedBlock.getTime();
            if (lastTime == null) {
                duration = Duration.ZERO;
            } else {
                duration = Duration.between(lastTime, newLastTime);

            }
            lastTime = newLastTime;

            msgIt = recordedBlock.getMessages().iterator();
            Validate.isTrue(msgIt.hasNext()); // An empty recordedBlock should never get written! There should always be something to read
                                              // at the end of this if block, because as soon as we come out of it we'll try to call .next()
        }

        RecordedMessage recordedMessage = msgIt.next();
        Validate.notNull(recordedMessage.getSrcAddress());
        // recordedMessage.dstsuffix may be null
        Validate.notNull(recordedMessage.getMessage());
        return new SourceMessage(
                recordedMessage.getSrcAddress(),
                AddressUtils.parentize(destinationPrefix, recordedMessage.getDstSuffix()),
                duration,
                recordedMessage.getMessage());
    }

    @Override
    public void close() throws Exception {
        IOUtils.closeQuietly(dis);
    }
    
}
