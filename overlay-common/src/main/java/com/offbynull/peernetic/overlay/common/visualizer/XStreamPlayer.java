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
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

/**
 * Plays steps recorded with a {@link XStreamRecorder}.
 * @author Kasra F
 * @param <A> address type
 */
public final class XStreamPlayer<A> implements Player<A> {

    private DataInputStream is;
    private XStream xstream;

    /**
     * Constructs a {@link XStreamPlayer} object.
     * @param is input stream
     * @throws NullPointerException if any arguments are {@code null}
     */
    public XStreamPlayer(InputStream is) {
        Validate.notNull(is);

        this.is = new DataInputStream(is);
        xstream = new XStream();
    }

    @Override
    public void play(Visualizer<A> visualizer) {
        Validate.notNull(visualizer);

        try {
            Long lastTime = null;
            while (true) {
                try {
                    switch (is.read()) {
                        case -1:
                            return;
                        case 0:
                            break;
                        default:
                            throw new IllegalStateException();
                    }

                    long time = (Long) read();
                    if (lastTime != null) {
                        Thread.sleep(time - lastTime);
                    }
                    lastTime = time;

                    String output = (String) read();
                    Command<A>[] commands = (Command<A>[]) read();

                    visualizer.step(output, commands);
                } catch (IOException | InterruptedException | XStreamException e) {
                    throw new IllegalStateException(e);
                }
            }
        } finally {
            IOUtils.closeQuietly(is);
        }

    }
    
    private Object read() throws IOException {
        int len = is.readInt();
        byte[] data = new byte[len];
        IOUtils.readFully(is, data);
        
        return xstream.fromXML(new String(data, "UTF-8"));
    }

}
