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
package com.offbynull.peernetic.overlay.common.visualizer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

/**
 * Records steps that can later be played with an {@link XStreamPlayer}.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class XStreamRecorder<A> implements Recorder<A> {
    private XStream xstream;
    private DataOutputStream os;

    /**
     * Constructs a {@link XStreamRecorder} object.
     * @param os output stream
     * @throws NullPointerException if any arguments are {@code null}
     */
    public XStreamRecorder(OutputStream os) {
        Validate.notNull(os);
        
        this.os = new DataOutputStream(os);
        xstream = new XStream();
    }
    
    @Override
    public void recordStep(String output, Command<A>... commands) {
        try {
            os.write(0);
            write(System.currentTimeMillis());
            write(output);
            write(commands);
        } catch (IOException | XStreamException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private void write(Object o) throws IOException {
        byte[] data = xstream.toXML(o).getBytes("UTF-8");
        os.writeInt(data.length);
        os.write(data);        
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(os);
    }
    
}
