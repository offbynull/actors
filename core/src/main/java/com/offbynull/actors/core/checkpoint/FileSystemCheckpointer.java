/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
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
package com.offbynull.actors.core.checkpoint;

import com.offbynull.actors.core.context.Serializer;
import com.offbynull.actors.core.context.SourceContext;
import com.offbynull.actors.core.shuttle.Address;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Saves and restores actors via the filesystem.
 * @author Kasra Faghihi
 */
public final class FileSystemCheckpointer implements Checkpointer {
    
    private static final Logger LOG = LoggerFactory.getLogger(FileSystemCheckpointer.class);
    
    private final Path directory;
    private final Serializer serializer;
    private final ExecutorService executor;

    /**
     * Create a {@link FileSystemCheckpointer} object.
     * @param serializer serializer to use for saving/restoring actors
     * @param directory storage directory for serialized actors
     * @return new instance of {@link FileSystemCheckpointer}
     */
    public static FileSystemCheckpointer create(Serializer serializer, Path directory) {
        Validate.notNull(serializer);
        Validate.notNull(directory);
        
        try {
            Files.createDirectories(directory);
        } catch (IOException ioe) {
            throw new IllegalArgumentException(ioe);
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(
                1,
                new BasicThreadFactory.Builder()
                        .daemon(true)
                        .namingPattern(FileSystemCheckpointer.class.getSimpleName() + " push thread")
                        .build());
        
        try {
            return new FileSystemCheckpointer(serializer, directory, executor);
        } catch (RuntimeException e) {
            executor.shutdownNow();
            throw e;
        }
    }
    
    private FileSystemCheckpointer(Serializer serializer, Path directory, ExecutorService executor) {
        Validate.notNull(serializer);
        Validate.notNull(directory);
        Validate.notNull(executor);
        this.serializer = serializer;
        this.directory = directory;
        this.executor = executor;
    }

    @Override
    public Future<Void> checkpoint(SourceContext ctx) {
        Validate.notNull(ctx);
        Validate.isTrue(ctx.isRoot());
        
        Address addr = ctx.self();
        byte[] data = serializer.serialize(ctx);
        
        return executor.submit(new FileWriteCallable(directory, addr, data));
    }

    @Override
    public Future<Void> delete(Address address) {
        Validate.notNull(address);
        
        return executor.submit(new FileDeleteCallable(directory, address));
    }

    @Override
    public RestoreResultIterator restore() {
        try {
            return new FileRestoreResultIterator(serializer, directory);
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    @Override
    public void close() throws Exception {
        executor.shutdownNow();
    }

    private static final class FileWriteCallable implements Callable<Void> {

        private final Path directory;
        private final Address address;
        private final byte[] data;

        public FileWriteCallable(Path directory, Address address, byte[] data) {
            this.directory = directory;
            this.address = address;
            this.data = data;
        }

        @Override
        public Void call() throws Exception {
            String filename;
            try {
                filename = URLEncoder.encode(address.toString(), "UTF-8");
            } catch (UnsupportedEncodingException use) {
                LOG.error("Unable to encode filename {}", address, use);
                throw use;
            }

            Path filepath = directory.resolve(filename);
            try {
                Files.write(filepath, data);
            } catch (IOException ioe) {
                LOG.error("Unable to write file {}", filepath, ioe);
                throw ioe;
            }
            
            return null;
        }
    }

    private static final class FileDeleteCallable implements Callable<Void> {

        private final Path directory;
        private final Address address;

        public FileDeleteCallable(Path directory, Address address) {
            this.directory = directory;
            this.address = address;
        }

        @Override
        public Void call() throws Exception {
            String filename;
            try {
                filename = URLEncoder.encode(address.toString(), "UTF-8");
            } catch (UnsupportedEncodingException use) {
                LOG.error("Unable to encode filename {}", address, use);
                throw use;
            }

            Path filepath = directory.resolve(filename);
            try {
                Files.delete(filepath);
            } catch (IOException ioe) {
                LOG.error("Unable to write file {}", filepath, ioe);
                throw ioe;
            }
            
            return null;
        }
    }
    
    private static final class FileRestoreResultIterator implements RestoreResultIterator {

        private final Serializer serializer;
        private final DirectoryStream<Path> stream;
        private final Iterator<Path> streamIt;

        public FileRestoreResultIterator(Serializer serializer, Path directory) throws IOException {
            this.serializer = serializer;
            stream = Files.newDirectoryStream(directory);
            streamIt = stream.iterator();
        }
        
        @Override
        public SourceContext next() {
            while (streamIt.hasNext()) {
                Path filepath = streamIt.next();
                try {
                    String filename = filepath.getFileName().toString();
                    String addrStr = URLDecoder.decode(filename, "UTF-8");
                    Address.fromString(addrStr); // sanity test, result isn't used
                } catch (IllegalArgumentException | UnsupportedEncodingException e) {
                    LOG.error("Unable to decode filename {}", filepath.toString(), e);
                    continue;
                }
                
                byte[] data;
                try {
                    data = Files.readAllBytes(filepath);
                } catch (IOException ioe) {
                    LOG.error("Unable to read file {}", filepath, ioe);
                    continue;
                }
                
                SourceContext ctx;
                try {
                    ctx = serializer.unserialize(data);
                } catch (IllegalArgumentException iae) {
                    LOG.error("Unable to unserialize file {}", filepath, iae);
                    continue;
                }


                if (!ctx.isRoot()) {
                    LOG.error("Unserialized non-root actor {}", filepath); // should never really happen? we test for root when we write
                    continue;
                }
                
                return ctx;
            }
            
            return null;
        }

        @Override
        public void close() throws Exception {
            stream.close();
        }
        
    }
}
