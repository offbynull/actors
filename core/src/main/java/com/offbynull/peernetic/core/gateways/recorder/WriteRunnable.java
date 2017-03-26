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
package com.offbynull.peernetic.core.gateways.recorder;

import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.common.Serializer;
import com.offbynull.peernetic.core.shuttle.Address;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class WriteRunnable implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(WriteRunnable.class);

    private final WriteBus bus;
    private final File file;
    private final Address selfPrefix;
    private final Serializer serializer;

    WriteRunnable(File file, String selfPrefix, Serializer serializer) {
        Validate.notNull(file);
        Validate.notNull(selfPrefix);
        Validate.notNull(serializer);

        this.bus = new WriteBus();
        this.file = file;
        this.selfPrefix = Address.of(selfPrefix);
        this.serializer = serializer;
    }

    public WriteBus getBus() {
        return bus;
    }

    @Override
    public void run() {
        LOG.info("Started writing");
        try (FileOutputStream fos = new FileOutputStream(file);
                DataOutputStream dos = new DataOutputStream(fos)) {
            try {
                while (true) {
                    MessageBlock messageBlock = bus.pull();

                    LOG.debug("Writing block of {} messages", messageBlock.getMessages().size());
                    writeMessageBlock(messageBlock, dos);
                }
            } catch (InterruptedException ie) {
                LOG.info("Stopping write thread (interrupted)");
                Thread.interrupted(); // remove interrupted flag cause we're going to be flushing remaining items here

                bus.drain().forEach(x -> {
                    try {
                        LOG.debug("Flushing message block");
                        writeMessageBlock(x, dos);
                    } catch (IOException ioe) {
                        LOG.error("Error in write thread while flushing", ioe);
                    }
                });
                
                writeTermination(dos);
            }
        } catch (Exception e) {
            LOG.error("Internal error encountered", e);
        } finally {
            bus.close(); // so stuff doesn't keep accumulating in this internalbus
        }
    }

    private void writeMessageBlock(MessageBlock messageBlock, final DataOutputStream dos) throws IOException {
        Instant time = messageBlock.getTime();
        UnmodifiableList<Message> messages = messageBlock.getMessages();

        List<RecordedMessage> recordedMessages = messages.stream()
                 // only msgs that are destined for output
                .filter(x -> selfPrefix.isPrefixOf(x.getDestinationAddress()))
                .map(x -> {
                    return new RecordedMessage(
                            x.getSourceAddress(),
                            selfPrefix.appendSuffix(x.getDestinationAddress()),
                            x.getMessage());
                })
                .collect(Collectors.toList());

        RecordedBlock recordedBlock = new RecordedBlock(recordedMessages, time);

        byte[] data = serializer.serialize(recordedBlock);
        dos.writeBoolean(true);
        dos.writeInt(data.length);
        IOUtils.write(data, dos);
        dos.flush();
    }

    private void writeTermination(final DataOutputStream dos) throws IOException {
        dos.writeBoolean(false);
        dos.flush();
    }

}