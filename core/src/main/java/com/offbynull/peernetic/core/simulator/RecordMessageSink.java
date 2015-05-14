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
package com.offbynull.peernetic.core.simulator;

import com.offbynull.peernetic.core.common.Serializer;
import com.offbynull.peernetic.core.gateways.recorder.RecordedBlock;
import com.offbynull.peernetic.core.gateways.recorder.RecordedMessage;
import com.offbynull.peernetic.core.gateways.recorder.ReplayerGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

/**
 * A {@link MessageSink} that records messages to a file. Recorded messages can be read back either via {@link ReplayMessageSource} (for
 * simulations) or {@link ReplayerGateway} (for real runs).
 * @author Kasra Faghihi
 */
public final class RecordMessageSink implements MessageSink { // FYI: NOT THREAD-SAFE
    private final Address destinationPrefix;
    private final DataOutputStream dos;
    private final Serializer serializer;
    
    private boolean closed;

    /**
     * Constructs a {@link RecordMessageSink} object.
     * @param destinationPrefix destination address prefix for messages to be written
     * @param file file messages should be written to
     * @param serializer serializer to use for serializing messages
     * @throws IOException if {@code file} could not be opened for writing
     * @throws NullPointerException if any argument is {@code null}
     */
    public RecordMessageSink(String destinationPrefix, File file, Serializer serializer) throws IOException {
        Validate.notNull(destinationPrefix);
        Validate.notNull(file);
        Validate.notNull(serializer);
        this.destinationPrefix = Address.of(destinationPrefix);
        this.dos = new DataOutputStream(new FileOutputStream(file));
        this.serializer = serializer;
    }
    
    @Override
    public void writeNextMessage(Address source, Address destination, Instant time, Object message) throws IOException {
        Validate.isTrue(!destination.isEmpty());
        if (!destinationPrefix.isPrefixOf(destination)) {
            return;
        }
        
        RecordedMessage recordedMessage = new RecordedMessage(
                source,
                destination.removePrefix(destinationPrefix),
                message);
        RecordedBlock recordedBlock = new RecordedBlock(Collections.singletonList(recordedMessage), time);

        byte[] data = serializer.serialize(recordedBlock);
        dos.writeBoolean(true);
        dos.writeInt(data.length);
        IOUtils.write(data, dos);
    }

    @Override
    public void close() throws Exception {
        if (closed) {
            return;
        }
        
        try {
            dos.writeBoolean(false);
        } catch (IOException ioe) {
            throw ioe;
        } finally {
            closed = true;
            IOUtils.closeQuietly(dos);
        }
    }
    
}
