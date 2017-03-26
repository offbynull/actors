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
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.common.Serializer;
import com.offbynull.peernetic.core.shuttle.Address;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ReadRunnable implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ReadRunnable.class);

    private final Shuttle dstShuttle;
    private final File file;
    private final Address dstAddress;
    private final Serializer serializer;

    ReadRunnable(Shuttle dstShuttle, Address dstAddress, File file, Serializer serializer) {
        Validate.notNull(dstShuttle);
        Validate.notNull(file);
        Validate.notNull(serializer);
        Validate.isTrue(!dstAddress.isEmpty());
        Validate.isTrue(Address.of(dstShuttle.getPrefix()).isPrefixOf(dstAddress));
        this.dstShuttle = dstShuttle;
        this.file = file;
        this.dstAddress = dstAddress;
        this.serializer = serializer;
    }

    @Override
    public void run() {
        LOG.info("Started reading");
        try (FileInputStream fis = new FileInputStream(file);
                DataInputStream dis = new DataInputStream(fis)) {
            Instant lastTime = null;
            while (true) {
                boolean hasMore = dis.readBoolean();
                if (!hasMore) {
                    LOG.debug("End of read (EOF marker found) -- finishing thread");
                    return;
                }

                int size = dis.readInt();
                byte[] data = new byte[size];
                
                IOUtils.readFully(dis, data);
                RecordedBlock recordedBlock = (RecordedBlock) serializer.deserialize(data);
                
                if (lastTime != null) {
                    Duration duration = Duration.between(lastTime, recordedBlock.getTime());
                    Thread.sleep(duration.toMillis());
                }
                
                LOG.debug("Writing block of {} messages", recordedBlock.getMessages().size());
                
                List<Message> messages
                        = recordedBlock.getMessages().stream()
                        .map(x -> {
                            Address dstSuffix = x.getDstSuffix();
                            return new Message(
                                    x.getSrcAddress(),
                                    dstSuffix == null ? dstAddress : dstAddress.appendSuffix(dstSuffix),
                                    x.getMessage());
                        })
                        .collect(Collectors.toList());
                dstShuttle.send(messages);
                
                lastTime = recordedBlock.getTime();
            }
            
        } catch (EOFException e) {
            LOG.error("Unexpected end of file");
        } catch (InterruptedException ie) {
            LOG.error("Stopping read thread (interrupted)");
        } catch (Exception e) {
            LOG.error("Internal error encountered", e);
        }
    }

}