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
package com.offbynull.actors.core.cache;

import com.offbynull.actors.core.checkpoint.FileSystemCheckpointer;
import com.offbynull.actors.core.context.Serializer;
import com.offbynull.actors.core.context.SourceContext;
import com.offbynull.actors.core.shuttle.Address;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caches and restores actors via the filesystem.
 *
 * @author Kasra Faghihi
 */
public final class FileSystemCacher implements Cacher {

    private static final Logger LOG = LoggerFactory.getLogger(FileSystemCacher.class);

    private final Path directory;
    private final Serializer serializer;

    /**
     * Create a {@link FileSystemCheckpointer} object.
     *
     * @param serializer serializer to use for saving/restoring actors
     * @param directory storage directory for serialized actors
     * @return new instance of {@link FileSystemCheckpointer}
     */
    public static FileSystemCacher create(Serializer serializer, Path directory) {
        Validate.notNull(serializer);
        Validate.notNull(directory);

        try {
            Files.createDirectories(directory);
        } catch (IOException ioe) {
            throw new IllegalArgumentException(ioe);
        }

        return new FileSystemCacher(serializer, directory);
    }

    private FileSystemCacher(Serializer serializer, Path directory) {
        Validate.notNull(serializer);
        Validate.notNull(directory);
        this.serializer = serializer;
        this.directory = directory;
    }

    @Override
    public boolean cache(SourceContext ctx) {
        Validate.notNull(ctx);
        Validate.isTrue(ctx.isRoot());

        Address address = ctx.self();
        byte[] data = serializer.serialize(ctx);

        String filename;
        try {
            filename = URLEncoder.encode(address.toString(), "UTF-8");
        } catch (UnsupportedEncodingException use) {
            LOG.error("Unable to encode filename {}", address, use);
            return false;
        }

        Path filepath = directory.resolve(filename);
        try {
            FileUtils.writeByteArrayToFile(filepath.toFile(), data);
        } catch (IOException ioe) {
            LOG.error("Unable to write file {}", filepath, ioe);
            return false;
        }

        return true;
    }

    @Override
    public SourceContext load(Address address) {
        Validate.notNull(address);
        
        String filename;
        try {
            filename = URLEncoder.encode(address.toString(), "UTF-8");
        } catch (UnsupportedEncodingException use) {
            LOG.error("Unable to encode filename {}", address, use);
            return null;
        }

        Path filepath = directory.resolve(filename);
        byte[] data;
        try {
            data = FileUtils.readFileToByteArray(filepath.toFile());
        } catch (IOException ioe) {
            LOG.error("Unable to read file {}", filepath, ioe);
            return null;
        }

        SourceContext ctx;
        try {
            ctx = serializer.unserialize(data);
        } catch (IllegalArgumentException iae) {
            LOG.error("Unable to unserialize file {}", filepath, iae);
            return null;
        }
        
        Validate.isTrue(ctx.isRoot()); // sanity check... because we test when we cache, this would never happen

        return ctx;
    }

    @Override
    public void close() {
        // do nothing
    }

}
